package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SoftDeleteTest : IntegrationTestBase() {

  @Test
  fun `soft delete removes events, timeline entries, and answers after the given point in time`() {
    val tCreate = LocalDateTime.parse("2020-01-01T09:00:00")
    val tBefore = LocalDateTime.parse("2020-01-01T10:00:00")
    val tAfter1 = LocalDateTime.parse("2020-01-01T11:00:00")
    val tAfter2 = LocalDateTime.parse("2020-01-01T12:00:00")
    val softDeleteFrom = LocalDateTime.parse("2020-01-01T10:30:00")

    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(
      backdatedCommand(
        tCreate,
        CreateAssessmentCommand(
          user = testUserDetails,
          assessmentType = "TEST",
          formVersion = "1",
        ),
      ).commands[0].result,
    ).assessmentUuid

    backdatedCommand(
      tBefore,
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid.toReference(),
        added = mapOf("q-before" to SingleValue("a-before")),
        removed = emptyList(),
      ),
    )

    backdatedCommand(
      tAfter1,
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid.toReference(),
        added = mapOf("q-after-1" to SingleValue("a-after-1")),
        removed = emptyList(),
      ),
    )

    backdatedCommand(
      tAfter2,
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid.toReference(),
        added = mapOf("q-after-2" to SingleValue("a-after-2")),
        removed = emptyList(),
      ),
    )

    val versionBeforeSoftDelete = assertIs<AssessmentVersionQueryResult>(
      query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )
    assertEquals(3, versionBeforeSoftDelete.answers.size)

    val timelineBeforeSoftDelete = assertIs<TimelineQueryResult>(
      query(TimelineQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )
    val timelineCountBefore = timelineBeforeSoftDelete.timeline.size

    command(
      SoftDeleteCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid.toReference(),
        pointInTime = softDeleteFrom,
      ),
    )

    val versionAfterSoftDelete = assertIs<AssessmentVersionQueryResult>(
      query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    assertEquals(1, versionAfterSoftDelete.answers.size)
    assertEquals(SingleValue("a-before"), versionAfterSoftDelete.answers["q-before"])
    assertNull(versionAfterSoftDelete.answers["q-after-1"])
    assertNull(versionAfterSoftDelete.answers["q-after-2"])

    val timelineAfterSoftDelete = assertIs<TimelineQueryResult>(
      query(TimelineQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    assertEquals(timelineCountBefore - 2, timelineAfterSoftDelete.timeline.size)
    timelineAfterSoftDelete.timeline.forEach {
      assertEquals(true, !it.timestamp.isAfter(softDeleteFrom))
    }
  }
}
