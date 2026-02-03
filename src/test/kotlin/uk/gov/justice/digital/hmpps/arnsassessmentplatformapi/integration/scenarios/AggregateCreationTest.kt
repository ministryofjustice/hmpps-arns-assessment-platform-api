package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class AggregateCreationTest : IntegrationTestBase() {

  @Test
  fun `new aggregate is created for point-in-time`() {
    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(
      command(
        CreateAssessmentCommand(
          user = testUserDetails,
          assessmentType = "TEST",
          formVersion = "1",
        ),
      ).commands[0].result,
    ).assessmentUuid

    val pointsInTime = mutableMapOf(
      "event-1" to assertIs<AssessmentVersionQueryResult>(
        query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
          .expectStatus().isOk
          .expectBody(QueriesResponse::class.java)
          .returnResult()
          .responseBody!!
          .queries.first().result,
      ),
    )

    for (i in 3..51) {
      command(
        UpdateAssessmentAnswersCommand(
          user = testUserDetails,
          assessmentUuid = assessmentUuid,
          added = mapOf("event-$i" to SingleValue("answer-$i")),
          removed = emptyList(),
        ),
      )

      pointsInTime["event-$i"] = assertIs<AssessmentVersionQueryResult>(
        query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
          .expectStatus().isOk
          .expectBody(QueriesResponse::class.java)
          .returnResult()
          .responseBody!!
          .queries.first().result,
      ).also {
        assertEquals(i - 2, it.answers.size)
        assertEquals(SingleValue("answer-$i"), it.answers["event-$i"])
      }
    }

    for (i in 3..50) {
      assertEquals(
        pointsInTime["event-1"]!!.aggregateUuid,
        pointsInTime["event-$i"]!!.aggregateUuid,
        "Same aggregate for event $i",
      )
    }

    assertNotEquals(
      pointsInTime["event-1"]!!.aggregateUuid,
      pointsInTime["event-51"]!!.aggregateUuid,
      "New aggregate for event 51",
    )

    val recreated = assertIs<AssessmentVersionQueryResult>(
      query(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessmentUuid),
          timestamp = pointsInTime["event-49"]!!.updatedAt,
        ),
      )
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    for (i in 3..51) {
      assertNotEquals(
        recreated.aggregateUuid,
        pointsInTime["event-$i"]!!.aggregateUuid,
        "Aggregate for point in time [49] - different from previous aggregate for event $i",
      )
    }
  }
}
