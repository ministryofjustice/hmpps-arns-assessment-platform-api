package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidQueryException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsSoftDeletedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsSoftDeletedSinceQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import java.time.LocalDateTime
import java.util.UUID

class GetAssessmentsSoftDeletedSinceHandlerTest {
  private val clock: Clock = mockk()
  private val eventService: EventService = mockk()

  private val services = QueryHandlerServiceBundle(
    assessment = mockk(),
    state = mockk(),
    userDetails = mockk(),
    timeline = mockk(),
    event = eventService,
    clock = clock,
  )

  private val now = LocalDateTime.parse("2026-05-01T12:00:00")
  private val user = UserDetails(
    id = "FOO_USER",
    name = "Foo User",
  )

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { clock.now() } returns now
  }

  @Test
  fun `returns UUIDs for assessments soft deleted since cutoff`() {
    val type = "TEST_TYPE"
    val since = now.minusHours(6)

    val assessment1 = AssessmentEntity(type = type, createdAt = now.minusDays(2))
    val assessment2 = AssessmentEntity(type = type, createdAt = now.minusDays(1))

    every {
      eventService.findAssessmentsSoftDeletedSince(type, since)
    } returns listOf(assessment1, assessment2)

    val handler = GetAssessmentsSoftDeletedSinceHandler(
      services = services,
      maxLookbackDays = 1,
    )

    val query = GetAssessmentsSoftDeletedSinceQuery(
      assessmentType = type,
      since = since,
      user = user,
    )

    val result = handler.handle(query)

    assertEquals(
      GetAssessmentsSoftDeletedSinceQueryResult(
        assessments = listOf(assessment1.uuid, assessment2.uuid),
      ),
      result,
    )
  }

  @Test
  fun `returns empty list when no assessments were soft deleted`() {
    val type = "TEST_TYPE"
    val since = now.minusHours(6)

    every {
      eventService.findAssessmentsSoftDeletedSince(type, since)
    } returns emptyList()

    val handler = GetAssessmentsSoftDeletedSinceHandler(
      services = services,
      maxLookbackDays = 1,
    )

    val query = GetAssessmentsSoftDeletedSinceQuery(
      assessmentType = type,
      since = since,
      user = user,
    )

    val result = handler.handle(query)

    assertEquals(
      GetAssessmentsSoftDeletedSinceQueryResult(
        assessments = emptyList(),
      ),
      result,
    )
  }

  @Test
  fun `throws when since exceeds max lookback`() {
    val handler = GetAssessmentsSoftDeletedSinceHandler(
      services = services,
      maxLookbackDays = 1,
    )

    val query = GetAssessmentsSoftDeletedSinceQuery(
      assessmentType = "TEST_TYPE",
      since = now.minusDays(2),
      user = user,
    )

    val exception = assertThrows<InvalidQueryException> {
      handler.handle(query)
    }

    assertEquals(
      "The 'since' parameter cannot be older than 1 day(s)",
      exception.developerMessage,
    )
  }

  @Test
  fun `does not enforce max lookback when configured as zero`() {
    val type = "TEST_TYPE"
    val since = now.minusDays(30)
    val assessment = AssessmentEntity(type = type, createdAt = now.minusDays(40))

    every {
      eventService.findAssessmentsSoftDeletedSince(type, since)
    } returns listOf(assessment)

    val handler = GetAssessmentsSoftDeletedSinceHandler(
      services = services,
      maxLookbackDays = 0,
    )

    val query = GetAssessmentsSoftDeletedSinceQuery(
      assessmentType = type,
      since = since,
      user = user,
    )

    val result = handler.handle(query)

    assertEquals(
      GetAssessmentsSoftDeletedSinceQueryResult(
        assessments = listOf(assessment.uuid),
      ),
      result,
    )
  }
}