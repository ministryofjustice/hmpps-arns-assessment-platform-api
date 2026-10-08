package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UndeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UndeleteTest : IntegrationTestBase() {

  private fun createAssessment(at: LocalDateTime): UUID = assertIs<CreateAssessmentCommandResult>(
    backdatedCommand(
      at,
      CreateAssessmentCommand(
        user = testUserDetails,
        assessmentType = "TEST",
        formVersion = "1",
        properties = mapOf("PLAN_TYPE" to SingleValue("INITIAL")),
      ),
    ).commands[0].result,
  ).assessmentUuid

  private fun answer(assessmentUuid: UUID, at: LocalDateTime, question: String) = backdatedCommand(
    at,
    UpdateAssessmentAnswersCommand(
      user = testUserDetails,
      assessmentUuid = assessmentUuid.toReference(),
      added = mapOf(question to SingleValue("answer")),
      removed = emptyList(),
    ),
  )

  private fun latestVersion(assessmentUuid: UUID) = assertIs<AssessmentVersionQueryResult>(
    query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
      .expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .returnResult()
      .responseBody!!
      .queries.first().result,
  )

  private fun commandExpectingStatus(status: HttpStatus, command: RequestableCommand) = webTestClient.post().uri("/command")
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
    .bodyValue(CommandsRequest(listOf(command)))
    .exchange()
    .expectStatus().isEqualTo(status)

  private fun timelineSize(assessmentUuid: UUID) = assertIs<TimelineQueryResult>(
    query(TimelineQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
      .expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .returnResult()
      .responseBody!!
      .queries.first().result,
  ).timeline.size

  @Test
  fun `undelete restores the events, timeline entries, and answers removed by a soft delete`() {
    val assessmentUuid = createAssessment(LocalDateTime.parse("2020-01-01T09:00:00"))
    answer(assessmentUuid, LocalDateTime.parse("2020-01-01T10:00:00"), "q-before")
    answer(assessmentUuid, LocalDateTime.parse("2020-01-01T11:00:00"), "q-after")
    val pointInTime = LocalDateTime.parse("2020-01-01T10:30:00")
    val timelineSizeBefore = timelineSize(assessmentUuid)

    command(SoftDeleteCommand(user = testUserDetails, assessmentUuid = assessmentUuid.toReference(), pointInTime = pointInTime))

    assertNull(latestVersion(assessmentUuid).answers["q-after"])

    command(UndeleteCommand(user = testUserDetails, assessmentUuid = assessmentUuid.toReference(), pointInTime = pointInTime))

    val versionAfterUndelete = latestVersion(assessmentUuid)
    assertEquals(SingleValue("answer"), versionAfterUndelete.answers["q-before"])
    assertEquals(SingleValue("answer"), versionAfterUndelete.answers["q-after"])
    assertEquals(timelineSizeBefore, timelineSize(assessmentUuid))
  }

  @Test
  fun `a soft delete from before the assessment existed removes everything, and undelete restores it all`() {
    val assessmentUuid = createAssessment(LocalDateTime.parse("2020-01-01T09:00:00"))
    answer(assessmentUuid, LocalDateTime.parse("2020-01-01T10:00:00"), "q-1")
    val epoch = LocalDateTime.parse("1970-01-01T00:00:00")

    command(SoftDeleteCommand(user = testUserDetails, assessmentUuid = assessmentUuid.toReference(), pointInTime = epoch))

    val versionAfterSoftDelete = latestVersion(assessmentUuid)
    assertNull(versionAfterSoftDelete.properties["PLAN_TYPE"])
    assertNull(versionAfterSoftDelete.answers["q-1"])

    command(UndeleteCommand(user = testUserDetails, assessmentUuid = assessmentUuid.toReference(), pointInTime = epoch))

    val versionAfterUndelete = latestVersion(assessmentUuid)
    assertEquals(SingleValue("INITIAL"), versionAfterUndelete.properties["PLAN_TYPE"])
    assertEquals(SingleValue("answer"), versionAfterUndelete.answers["q-1"])
  }

  @Test
  fun `undelete is refused and nothing changes when events were written after the soft delete`() {
    val assessmentUuid = createAssessment(LocalDateTime.parse("2020-01-01T09:00:00"))
    answer(assessmentUuid, LocalDateTime.parse("2020-01-01T10:00:00"), "q-before")
    answer(assessmentUuid, LocalDateTime.parse("2020-01-01T11:00:00"), "q-deleted")
    val pointInTime = LocalDateTime.parse("2020-01-01T10:30:00")

    command(SoftDeleteCommand(user = testUserDetails, assessmentUuid = assessmentUuid.toReference(), pointInTime = pointInTime))
    command(
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid.toReference(),
        added = mapOf("q-written-after" to SingleValue("answer")),
        removed = emptyList(),
      ),
    )

    commandExpectingStatus(
      HttpStatus.CONFLICT,
      UndeleteCommand(user = testUserDetails, assessmentUuid = assessmentUuid.toReference(), pointInTime = pointInTime),
    )

    val version = latestVersion(assessmentUuid)
    assertEquals(SingleValue("answer"), version.answers["q-before"])
    assertNull(version.answers["q-deleted"])
    assertEquals(SingleValue("answer"), version.answers["q-written-after"])
  }
}
