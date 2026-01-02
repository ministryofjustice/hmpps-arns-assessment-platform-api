package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.service

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.CreatedBy
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.CreatedByResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.oasys.persistence.OasysVersionRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.emptyList
import kotlin.test.assertEquals

class OasysVersionServiceTest {
  val repository: OasysVersionRepository = mockk()
  val service = OasysVersionService(repository)
  val persistedVersion = slot<OasysVersionEntity>()

  private val now: LocalDateTime = LocalDateTime.parse("2026-01-02T12:00:00")
  private val today: LocalDate = now.toLocalDate()
  private val yesterday: LocalDate = today.minusDays(1)

  @BeforeEach
  fun setup() {
    clearMocks(repository)
    mockkObject(Clock, CreatedByResolver)

    every { Clock.now() } returns now
    every { CreatedByResolver.from(any()) } returns CreatedBy.DAILY_EDIT

    every { repository.save(capture(persistedVersion)) } answers { firstArg() }
  }

  @Nested
  inner class CreateVersionFor {
    @Test
    fun `creates a version when no previous exists for the assessment`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithStatus("COUNTERSIGNED"),
      )

      every {
        repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid)
      } returns null

      service.createVersionFor(event)

      assertAll(
        { verify(exactly = 1) { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } },
        { verify(exactly = 1) { repository.save(any()) } },
        {
          assertNotNull(persistedVersion.captured)
          assertEquals(0, persistedVersion.captured.version)
          assertEquals(CreatedBy.DAILY_EDIT, persistedVersion.captured.createdBy)
          assertEquals("COUNTERSIGNED", persistedVersion.captured.status)
          assertSame(event, persistedVersion.captured.lastEvent)
          assertSame(assessment, persistedVersion.captured.assessment)
        },
      )
    }

    @Test
    fun `creates a version when no previous exists for the assessment - status UNSIGNED when event has no status`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithNoStatus(),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns null

      service.createVersionFor(event)

      assertEquals(0, persistedVersion.captured.version)
      assertEquals("UNSIGNED", persistedVersion.captured.status)
    }

    @Test
    fun `creates a version when a previous version exists but createdBy is not DAILY_EDIT`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      every { CreatedByResolver.from(any()) } returns CreatedBy.CREATED

      val latest = createOasysVersionWith(
        version = 3,
        status = "UNSIGNED",
        updatedAt = now.minusDays(0),
        assessment = assessment,
      )

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithStatus("COUNTERSIGNED"),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      assertAll(
        { assertEquals(4, persistedVersion.captured.version) },
        { assertEquals(CreatedBy.CREATED, persistedVersion.captured.createdBy) },
        { assertEquals("COUNTERSIGNED", persistedVersion.captured.status) }, // event status wins
        { assertSame(event, persistedVersion.captured.lastEvent) },
      )
    }

    @Test
    fun `creates a version when a previous version exists but updatedAt is on a different date`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val latest = createOasysVersionWith(
        version = 1,
        status = "OPEN",
        updatedAt = now.minusDays(1),
        assessment = assessment,
      )

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithStatus("COUNTERSIGNED"),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      assertEquals(2, persistedVersion.captured.version)
      assertEquals("COUNTERSIGNED", persistedVersion.captured.status)
    }

    @Test
    fun `updates today's version when createdBy is DAILY_EDIT`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val latest = createOasysVersionWith(
        version = 7,
        status = "UNSIGNED",
        updatedAt = now.minusHours(3),
        assessment = assessment,
      )

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithStatus("AWAITING_COUNTERSIGN"),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      // should update the *same instance* rather than create a new entity
      assertSame(latest, persistedVersion.captured)
      assertAll(
        { assertEquals(7, latest.version) },
        { assertEquals("AWAITING_COUNTERSIGN", latest.status) },
        { assertEquals(now, latest.updatedAt) },
        { assertSame(event, latest.lastEvent) },
      )
    }

    @Test
    fun `updates today's version when createdBy is DAILY_EDIT reusing the previous status`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val latest = createOasysVersionWith(
        version = 2,
        status = "UNSIGNED",
        updatedAt = now.minusMinutes(10),
        assessment = assessment,
      )

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithNoStatus(),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      assertEquals("UNSIGNED", latest.status)
      assertEquals(now, latest.updatedAt)
      assertSame(event, latest.lastEvent)
    }

    @Test
    fun `updates today's version when createdBy DAILY_EDIT defaulting to UNSIGNED when no previous or latest status`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val latest = createOasysVersionWith(
        version = 2,
        status = null,
        updatedAt = now.minusMinutes(10),
        assessment = assessment,
      )

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithNoStatus(),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      assertEquals("UNSIGNED", latest.status)
    }

    @Test
    fun `creates a new version when none available from today and re-uses the status`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val latest = createOasysVersionWith(
        version = 9,
        status = "AWAITING_COUNTERSIGN",
        updatedAt = now.minusDays(1),
        assessment = assessment,
      )

      val event = createEventWith(
        assessment = assessment,
        data = createPropertiesUpdatedEventWithNoStatus(),
      )

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      assertEquals(10, persistedVersion.captured.version)
      assertEquals("AWAITING_COUNTERSIGN", persistedVersion.captured.status)
    }

    @Test
    fun `creates a new version when none available from today defaulting to UNSIGNED when no previous or latest status`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      val latest = createOasysVersionWith(
        version = 0,
        status = null,
        updatedAt = now.minusDays(1),
        assessment = assessment,
      )

      val event = createEventWith(assessment = assessment, data = createPropertiesUpdatedEventWithNoStatus())

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns latest

      service.createVersionFor(event)

      assertEquals(1, persistedVersion.captured.version)
      assertEquals("UNSIGNED", persistedVersion.captured.status)
    }

    @Test
    fun `saves exactly once`() {
      val assessmentUuid = UUID.randomUUID()
      val assessment = getAssessmentWith(assessmentUuid)

      every { repository.findTopByAssessmentUuidOrderByVersionDesc(assessmentUuid) } returns null

      val event = createEventWith(assessment = assessment, data = createPropertiesUpdatedEventWithNoStatus())

      service.createVersionFor(event)

      verify(exactly = 1) { repository.save(any()) }
    }
  }

  @Nested
  inner class StatusOrNull {

    @Test
    fun `returns status when data is AssessmentPropertiesUpdatedEvent and STATUS is SingleValue`() {
      val assessment = getAssessmentWith(UUID.randomUUID())
      val event = createEventWith(assessment = assessment, data = createPropertiesUpdatedEventWithStatus("COUNTERSIGNED"))

      val status = with(service) { event.statusOrNull() }

      assertEquals("COUNTERSIGNED", status)
    }

    @Test
    fun `returns null when data is not AssessmentPropertiesUpdatedEvent`() {
      val assessment = getAssessmentWith(UUID.randomUUID())
      val event = createEventWith(
        assessment = assessment,
        data = AssessmentCreatedEvent(
          formVersion = "v1.0",
          properties = emptyMap(),
          timeline = null,
        ),
      )

      val status = with(service) { event.statusOrNull() }

      assertEquals(null, status)
    }

    @Test
    fun `returns null when STATUS key is missing`() {
      val assessment = getAssessmentWith(UUID.randomUUID())
      val data = AssessmentPropertiesUpdatedEvent(
        added = mapOf("SOMETHING_ELSE" to SingleValue("FOO")),
        removed = emptyList(),
        timeline = null,
      )
      val event = createEventWith(assessment = assessment, data = data)

      val status = with(service) { event.statusOrNull() }

      assertEquals(null, status)
    }

    @Test
    fun `returns null when STATUS value is not a SingleValue`() {
      val assessment = getAssessmentWith(UUID.randomUUID())
      val data = AssessmentPropertiesUpdatedEvent(
        added = mapOf("STATUS" to MultiValue(emptyList())),
        removed = emptyList(),
        timeline = null,
      )
      val event = createEventWith(assessment = assessment, data = data)

      val status = with(service) { event.statusOrNull() }

      assertEquals(null, status)
    }
  }

  private fun getAssessmentWith(uuid: UUID): AssessmentEntity = AssessmentEntity(uuid = uuid, type = "Test")

  private fun createEventWith(
    assessment: AssessmentEntity,
    data: Event,
  ): EventEntity<*> = EventEntity(
    assessment = assessment,
    data = data,
    user = User(id = "TEST_USER", name = "Test User"),
  )

  private fun createPropertiesUpdatedEventWithNoStatus(): AssessmentPropertiesUpdatedEvent = AssessmentPropertiesUpdatedEvent(
    added = emptyMap(),
    removed = emptyList(),
    timeline = null,
  )

  private fun createPropertiesUpdatedEventWithStatus(status: String): AssessmentPropertiesUpdatedEvent = AssessmentPropertiesUpdatedEvent(
    added = mapOf("STATUS" to SingleValue(status)),
    removed = emptyList(),
    timeline = null,
  )

  private fun createOasysVersionWith(
    version: Long,
    status: String?,
    updatedAt: LocalDateTime = now,
    assessment: AssessmentEntity,
  ): OasysVersionEntity = OasysVersionEntity(
    createdBy = CreatedBy.DAILY_EDIT, // only used for shouldUpdateLatest check via CreatedByResolver
    version = version,
    status = status,
    lastEvent = createEventWith(
      assessment,
      data = AssessmentCreatedEvent(
        formVersion = "v1.0",
        properties = emptyMap(),
        timeline = null,
      ),
    ),
    assessment = assessment,
  ).apply {
    this.updatedAt = updatedAt
  }
}
