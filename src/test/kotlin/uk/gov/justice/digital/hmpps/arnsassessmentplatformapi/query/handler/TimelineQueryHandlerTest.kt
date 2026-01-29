package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.TestableEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.criteria.TimelineCriteria
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import java.time.LocalDateTime
import java.util.UUID

class TimelineQueryHandlerTest {
  val assessment = AssessmentEntity(type = "TEST")
  val assessmentService: AssessmentService = mockk()
  val stateService: StateService = mockk()
  val userDetailsService: UserDetailsService = mockk()
  val timelineService: TimelineService = mockk()

  val services = QueryHandlerServiceBundle(
    assessment = assessmentService,
    state = stateService,
    userDetails = userDetailsService,
    timeline = timelineService,
  )
  val handler = TimelineQueryHandler(services)

  val now: LocalDateTime = LocalDateTime.now()

  val user = UserDetails(
    id = "FOO_USER",
    name = "Foo User",
  )

  val userEntity = UserDetailsEntity(
    uuid = UUID.randomUUID(),
    userId = "FOO_USER",
    displayName = "Foo User",
    authSource = AuthSource.NOT_SPECIFIED,
  )

  val fromTimestamp = LocalDateTime.parse("2026-01-01T12:00:00")
  val toTimestamp = LocalDateTime.parse("2026-01-28T12:00:00")

  val count: Int = 10
  val totalPages: Int = 5
  val pageNumber: Int = 0

  @BeforeEach
  fun setup() {
    clearAllMocks()
  }

  @Nested
  inner class QueryByUuid {
    val identifier = UuidIdentifier(assessment.uuid)

    @Test
    fun `returns the timeline for a given timeframe`() {
      val timelinePage: Page<TimelineEntity> = mockk()
      every { timelinePage.content } returns listOf(
        TimelineEntity(
          createdAt = LocalDateTime.parse("2026-01-02T12:00:00"),
          assessment = assessment,
          user = userEntity,
          eventType = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )
      every { timelinePage.totalPages } returns totalPages
      every { timelinePage.number } returns 0

      val expectedCriteria = TimelineCriteria(
        assessmentUuid = assessment.uuid,
        from = fromTimestamp,
        to = toTimestamp,
      )

      every {
        services.timeline.findAll(expectedCriteria, PageRequest.of(pageNumber, count))
      } returns timelinePage

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = TimelineQuery(
        user = user,
        timestamp = now,
        assessmentIdentifier = identifier,
        from = fromTimestamp,
        to = toTimestamp,
      )

      val result = handler.execute(query)

      assertEquals(
        TimelineQueryResult(
          timeline = listOf(
            TimelineItem(
              timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
              user = User(userEntity.uuid, user.name),
              event = TestableEvent::class.simpleName!!,
              data = mapOf("foo" to "bar"),
            ),
          ),
          pageInfo = PageInfo(
            pageNumber = pageNumber,
            totalPages = totalPages,
          ),
        ),
        result,
      )
    }

    @Test
    fun `returns a timeline containing a specified number of items`() {
      val timelinePage: Page<TimelineEntity> = mockk()
      every { timelinePage.content } returns listOf(
        TimelineEntity(
          createdAt = LocalDateTime.parse("2026-01-02T12:00:00"),
          assessment = assessment,
          user = userEntity,
          eventType = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )
      every { timelinePage.totalPages } returns totalPages
      every { timelinePage.number } returns 0

      every {
        services.timeline.findAll(
          TimelineCriteria(
            assessmentUuid = assessment.uuid,
          ),
          PageRequest.of(pageNumber, count),
        )
      } returns timelinePage

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = TimelineQuery(
        user = user,
        timestamp = now,
        assessmentIdentifier = identifier,
        from = fromTimestamp,
        to = toTimestamp,
      )

      val result = handler.execute(query)

      assertEquals(
        TimelineQueryResult(
          timeline = listOf(
            TimelineItem(
              timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
              user = User(userEntity.uuid, user.name),
              event = TestableEvent::class.simpleName!!,
              data = mapOf("foo" to "bar"),
            ),
          ),
          pageInfo = PageInfo(
            pageNumber,
            totalPages,
          ),
        ),
        result,
      )
    }
  }

  @Nested
  inner class QueryByExternalId {
    val identifier = ExternalIdentifier("A123456", IdentifierType.CRN, assessment.type)

    @Test
    fun `returns the timeline for a given timeframe`() {
      val timelinePage: Page<TimelineEntity> = mockk()
      every { timelinePage.content } returns listOf(
        TimelineEntity(
          createdAt = LocalDateTime.parse("2026-01-02T12:00:00"),
          assessment = assessment,
          user = userEntity,
          eventType = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )
      every { timelinePage.totalPages } returns totalPages
      every { timelinePage.number } returns 0

      every {
        services.timeline.findAll(
          TimelineCriteria(
            assessmentUuid = assessment.uuid,
            from = fromTimestamp,
            to = toTimestamp,
          ),
          PageRequest.of(pageNumber, count),
        )
      } returns timelinePage

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = TimelineQuery(
        user = user,
        timestamp = now,
        assessmentIdentifier = identifier,
        from = fromTimestamp,
        to = toTimestamp,
      )

      val result = handler.execute(query)

      assertEquals(
        TimelineQueryResult(
          timeline = listOf(
            TimelineItem(
              timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
              user = User(userEntity.uuid, user.name),
              event = TestableEvent::class.simpleName!!,
              data = mapOf("foo" to "bar"),
            ),
          ),
          pageInfo = PageInfo(
            pageNumber = pageNumber,
            totalPages = totalPages,
          ),
        ),
        result,
      )
    }

    @Test
    fun `returns a timeline containing a specified number of items`() {
      val timelinePage: Page<TimelineEntity> = mockk()
      every { timelinePage.content } returns listOf(
        TimelineEntity(
          createdAt = LocalDateTime.parse("2026-01-02T12:00:00"),
          assessment = assessment,
          user = userEntity,
          eventType = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )
      every { timelinePage.totalPages } returns totalPages
      every { timelinePage.number } returns 0

      every {
        services.timeline.findAll(
          TimelineCriteria(
            assessmentUuid = assessment.uuid,
          ),
          PageRequest.of(pageNumber, count),
        )
      } returns timelinePage

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = TimelineQuery(
        user = user,
        timestamp = now,
        assessmentIdentifier = identifier,
        from = fromTimestamp,
        to = toTimestamp,
      )

      val result = handler.execute(query)

      assertEquals(
        TimelineQueryResult(
          timeline = listOf(
            TimelineItem(
              timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
              user = User(userEntity.uuid, user.name),
              event = TestableEvent::class.simpleName!!,
              data = mapOf("foo" to "bar"),
            ),
          ),
          pageInfo = PageInfo(
            pageNumber,
            totalPages,
          ),
        ),
        result,
      )
    }
  }
}
