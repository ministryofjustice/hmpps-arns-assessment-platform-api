package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.TestableEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.PageWindow
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimeframeWindow
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.PageInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import java.time.LocalDateTime
import java.util.UUID

class AssessmentTimelineQueryHandlerTest {
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
  val handler = AssessmentTimelineQueryHandler(services)

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

  val timeframe = TimeframeWindow(
    from = LocalDateTime.parse("2026-01-01T12:00:00"),
    to = LocalDateTime.parse("2026-01-28T12:00:00"),
  )

  val count: Int = 10
  val totalPages: Int = 5
  val pageNumber: Int = 0
  val page = PageWindow(count, pageNumber)

  @BeforeEach
  fun setup() {
    clearAllMocks()
  }

  @Nested
  inner class QueryByUuid {
    val identifier = UuidIdentifier(assessment.uuid)

    @Test
    fun `returns the timeline for a given timeframe`() {
      val timeline = listOf(
        TimelineItem(
          timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
          user = User(userEntity.uuid, user.name),
          event = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )

      every {
        services.timeline.findAllBetweenByAssessmentUuid(
          assessment.uuid,
          timeframe.from,
          timeframe.to,
        )
      } returns timeline

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = AssessmentTimelineQuery(
        user = user,
        timestamp = now,
        identifier = identifier,
        window = timeframe,
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
        ),
        result,
      )
    }

    @Test
    fun `returns a timeline containing a specified number of items`() {
      val timeline: Page<TimelineItem> = mockk()
      every { timeline.content } returns listOf(
        TimelineItem(
          timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
          user = User(userEntity.uuid, user.name),
          event = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )
      every { timeline.totalPages } returns totalPages
      every { timeline.number } returns 0

      every {
        services.timeline.findAllPageableByAssessmentUuid(
          assessment.uuid,
          count,
          pageNumber,
        )
      } returns timeline

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = AssessmentTimelineQuery(
        user = user,
        timestamp = now,
        identifier = identifier,
        window = page,
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
      val timeline = listOf(
        TimelineItem(
          timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
          user = User(userEntity.uuid, user.name),
          event = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )

      every {
        services.timeline.findAllBetweenByAssessmentUuid(
          assessment.uuid,
          timeframe.from,
          timeframe.to,
        )
      } returns timeline

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = AssessmentTimelineQuery(
        user = user,
        timestamp = now,
        identifier = identifier,
        window = timeframe,
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
        ),
        result,
      )
    }

    @Test
    fun `returns a timeline containing a specified number of items`() {
      val timeline: Page<TimelineItem> = mockk()
      every { timeline.content } returns listOf(
        TimelineItem(
          timestamp = LocalDateTime.parse("2026-01-02T12:00:00"),
          user = User(userEntity.uuid, user.name),
          event = TestableEvent::class.simpleName!!,
          data = mapOf("foo" to "bar"),
        ),
      )
      every { timeline.totalPages } returns totalPages
      every { timeline.number } returns 0

      every {
        services.timeline.findAllPageableByAssessmentUuid(
          assessment.uuid,
          count,
          pageNumber,
        )
      } returns timeline

      every {
        services.assessment.findBy(identifier)
      } returns assessment

      val query = AssessmentTimelineQuery(
        user = user,
        timestamp = now,
        identifier = identifier,
        window = page,
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
