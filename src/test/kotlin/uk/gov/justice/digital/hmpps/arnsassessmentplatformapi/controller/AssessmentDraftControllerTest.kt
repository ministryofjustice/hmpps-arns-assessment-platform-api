package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import tools.jackson.databind.node.JsonNodeFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftQueryRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.SavedDraft
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.AssessmentDraftResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class AssessmentDraftControllerTest : IntegrationTestBase() {
  @Test
  fun `saves and returns only the current user's draft`() {
    val assessment = createAssessment()
    val request = draftRequest(assessment)
    val otherUserRequest = request.copy(user = UserDetails("OTHER_USER", "Other User"), draftKey = "goal:${UUID.randomUUID()}")

    val saved = save(assessment.assessmentUuid, request)
    save(assessment.assessmentUuid, otherUserRequest)

    assertThat(saved.revision).isEqualTo(1)
    assertThat(saved.data["goal_title"].asText()).isEqualTo("Find accommodation")

    val results = findAll(assessment.assessmentUuid, testUserDetails)

    assertThat(results).hasSize(1)
    assertThat(results.single().draftKey).isEqualTo(request.draftKey)
  }

  @Test
  fun `rejects an autosave revision that is not newer`() {
    val assessment = createAssessment()
    val request = draftRequest(assessment)
    save(assessment.assessmentUuid, request)

    webTestClient.put().uri("/assessment/${assessment.assessmentUuid}/draft")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isEqualTo(409)
  }

  @Test
  fun `removes the acknowledged draft after a successful committed save`() {
    val assessment = createAssessment()
    val request = draftRequest(assessment)
    save(assessment.assessmentUuid, request)

    val command = UpdateAssessmentAnswersCommand(
      user = testUserDetails,
      assessmentUuid = assessment.assessmentUuid.toReference(),
      added = mapOf("foo" to SingleValue("bar")),
      removed = emptyList(),
    )
    val commandRequest = CommandsRequest(
      commands = listOf(command),
      savedDraft = SavedDraft(
        assessmentUuid = assessment.assessmentUuid,
        user = testUserDetails,
        draftKey = request.draftKey,
        formVersion = request.formVersion,
        revision = request.revision,
      ),
    )

    webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(commandRequest)
      .exchange()
      .expectStatus().isOk

    assertThat(findAll(assessment.assessmentUuid, testUserDetails)).isEmpty()
  }

  private fun createAssessment(): AssessmentVersionQueryResult {
    val response = command(
      CreateAssessmentCommand(
        user = testUserDetails,
        assessmentType = "TEST",
        formVersion = "1.0",
      ),
    )
    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(response.commands.single().result).assessmentUuid
    return assertIs<AssessmentVersionQueryResult>(
      query(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessmentUuid),
        ),
      ).expectStatus().isOk
        .expectBody(uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse::class.java)
        .returnResult()
        .responseBody
        ?.queries
        ?.single()
        ?.result,
    )
  }

  private fun draftRequest(assessment: AssessmentVersionQueryResult) = AssessmentDraftRequest(
    user = testUserDetails,
    draftKey = "goal:${UUID.randomUUID()}",
    formVersion = assessment.formVersion,
    baseAggregateUuid = assessment.aggregateUuid,
    baseAggregateVersion = assessment.aggregateVersion,
    revision = 1,
    data = JsonNodeFactory.instance.objectNode().put("goal_title", "Find accommodation"),
    expiresAt = LocalDateTime.now().plusDays(7),
  )

  private fun save(assessmentUuid: UUID, request: AssessmentDraftRequest): AssessmentDraftResponse = webTestClient
    .put().uri("/assessment/$assessmentUuid/draft")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus().isOk
    .expectBody(AssessmentDraftResponse::class.java)
    .returnResult()
    .responseBody!!

  private fun findAll(assessmentUuid: UUID, user: UserDetails): List<AssessmentDraftResponse> = webTestClient
    .post().uri("/assessment/$assessmentUuid/draft/query")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
    .bodyValue(AssessmentDraftQueryRequest(user))
    .exchange()
    .expectStatus().isOk
    .expectBodyList(AssessmentDraftResponse::class.java)
    .returnResult()
    .responseBody!!
}
