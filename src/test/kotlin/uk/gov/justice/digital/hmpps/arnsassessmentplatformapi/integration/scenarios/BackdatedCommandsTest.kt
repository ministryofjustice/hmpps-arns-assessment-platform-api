package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import org.junit.jupiter.api.Assertions.assertTrue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BackdatedCommandsTest : IntegrationTestBase() {

  @Test
  fun `commands can be backdated`() {
    val backdateTo = LocalDateTime.parse("2020-01-01T12:10:00")

    val response = assertIs<CommandsResponse>(
      backdatedCommand(
        backdateTo = backdateTo,
        CreateAssessmentCommand(
          user = testUserDetails,
          assessmentType = "TEST",
          formVersion = "1",
        ),
      ),
    ).commands[0].result as CreateAssessmentCommandResult

    val assessment = assertIs<AssessmentVersionQueryResult>(
      query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(response.assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    assertBackdated(backdateTo, assessment.createdAt)
    assertBackdated(backdateTo, assessment.updatedAt)

    val timeline = assertIs<TimelineQueryResult>(
      query(TimelineQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(response.assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    assertEquals(2, timeline.timeline.size)
    timeline.timeline.forEach {
      assertBackdated(backdateTo, it.timestamp)
    }
  }

  private fun assertBackdated(backdateTo: LocalDateTime, actual: LocalDateTime) {
    // Allow small tolerance due to execution timing
    val difference = Duration.between(backdateTo, actual).abs()
    assertTrue(difference.seconds < 2)
  }

//  @Test // TODO
//  fun `commands cannot be backdated if newer events exist`() {
//    val response = assertIs<CommandsResponse>(command(
//      CreateAssessmentCommand(
//        user = testUserDetails,
//        assessmentType = "TEST",
//        formVersion = "1",
//      ),
//    )).commands[0].result as CreateAssessmentCommandResult
//
//    val backdateTo = LocalDateTime.parse("2020-01-01T12:10:00")
//    val updateAssessment = UpdateAssessmentAnswersCommand(
//      user = testUserDetails,
//      assessmentUuid = response.assessmentUuid.toReference(),
//      added = mapOf("q-1" to SingleValue("a-1")),
//      removed = emptyList(),
//    )
//
//    webTestClient.post().uri("/command?backdateTo=$backdateTo")
//      .header(HttpHeaders.CONTENT_TYPE, "application/json")
//      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
//      .bodyValue(CommandsRequest(listOf(updateAssessment)))
//      .exchange()
//      .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
//  }
}
