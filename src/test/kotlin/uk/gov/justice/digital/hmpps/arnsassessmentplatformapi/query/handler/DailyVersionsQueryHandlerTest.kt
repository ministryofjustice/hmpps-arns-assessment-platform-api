package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.DailyVersionDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.DailyVersionsQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.DailyVersionsQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import java.time.LocalDateTime
import java.util.UUID

class DailyVersionsQueryHandlerTest {
  val assessmentService: AssessmentService = mockk()
  val timelineService: TimelineService = mockk()
  val clock: Clock = mockk()

  val services = QueryHandlerServiceBundle(
    assessment = assessmentService,
    state = mockk(),
    userDetails = mockk(),
    timeline = timelineService,
    clock = clock,
  )

  val handler = DailyVersionsQueryHandler(services)

  val now: LocalDateTime = LocalDateTime.now()

  val user = UserDetails(
    id = "FOO_USER",
    name = "Foo User",
  )

  val dailyVersions = listOf(
    DailyVersionDetails(
      createdAt = now,
      updatedAt = now,
      lastTimelineItemUuid = UUID.randomUUID(),
    ),
    DailyVersionDetails(
      createdAt = now,
      updatedAt = now,
      lastTimelineItemUuid = UUID.randomUUID(),
    ),
  )

  @BeforeEach
  fun setup() {
    clearAllMocks()
    every { clock.now() } returns now
  }

  @ParameterizedTest
  @MethodSource("identifierProvider")
  fun `returns the daily versions for a given timeframe`(identifier: AssessmentIdentifier) {
    every {
      services.timeline.findDailyVersions(assessment.uuid)
    } returns dailyVersions

    every {
      services.assessment.findBy(identifier, now)
    } returns assessment

    val query = DailyVersionsQuery(
      user = user,
      timestamp = now,
      assessmentIdentifier = identifier,
    )

    val result = handler.execute(query)

    assertEquals(
      DailyVersionsQueryResult(
        versions = dailyVersions,
      ),
      result,
    )
  }

  @ParameterizedTest
  @MethodSource("identifierProvider")
  fun `returns an empty list`(identifier: AssessmentIdentifier) {
    every {
      services.timeline.findDailyVersions(assessment.uuid)
    } returns emptyList()

    every {
      services.assessment.findBy(identifier, now)
    } returns assessment

    val query = DailyVersionsQuery(
      user = user,
      timestamp = now,
      assessmentIdentifier = identifier,
    )

    val result = handler.execute(query)

    assertEquals(
      DailyVersionsQueryResult(
        versions = emptyList(),
      ),
      result,
    )
  }

  companion object {
    val assessment = AssessmentEntity(type = "TEST")

    @JvmStatic
    fun identifierProvider() = listOf(
      UuidIdentifier(assessment.uuid),
      ExternalIdentifier("A123456", IdentifierType.CRN, assessment.type),
    )
  }
}
