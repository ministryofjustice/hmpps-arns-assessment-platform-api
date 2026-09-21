package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import tools.jackson.databind.node.JsonNodeFactory
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftQueryRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.AssessmentDraftRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.SavedDraft
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.AssessmentDraftResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

@DisplayName("Assessment Draft API Tests")
class AssessmentDraftApiTest : IntegrationTestBase() {
  @Test
  fun `saves retrieves rejects stale and removes an acknowledged draft`() {
    val user = UserDetails("draft-e2e-${UUID.randomUUID()}", "Draft E2E User")
    val assessment = createAssessment(user)
    val request = AssessmentDraftRequest(
      user = user,
      draftKey = "goal:${UUID.randomUUID()}",
      formVersion = assessment.formVersion,
      baseAggregateUuid = assessment.aggregateUuid,
      baseAggregateVersion = assessment.aggregateVersion,
      revision = 1,
      data = JsonNodeFactory.instance.objectNode().put("goal_title", "Find accommodation"),
      expiresAt = LocalDateTime.now().plusDays(1),
    )

    val saved = webTestClient.put().uri("/assessment/${assessment.assessmentUuid}/draft")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody<AssessmentDraftResponse>()
      .returnResult()
      .responseBody

    assertThat(saved?.revision).isEqualTo(request.revision)

    val drafts = findAll(assessment.assessmentUuid, user)
    assertThat(drafts).hasSize(1)
    assertThat(drafts.single().data["goal_title"].asText()).isEqualTo("Find accommodation")

    webTestClient.put().uri("/assessment/${assessment.assessmentUuid}/draft")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isEqualTo(409)

    val commandRequest = CommandsRequest(
      commands = listOf(
        UpdateAssessmentAnswersCommand(
          user = user,
          assessmentUuid = assessment.assessmentUuid.toReference(),
          added = mapOf("draft_test" to SingleValue("committed")),
          removed = emptyList(),
        ),
      ),
      savedDraft = SavedDraft(
        assessmentUuid = assessment.assessmentUuid,
        user = user,
        draftKey = request.draftKey,
        formVersion = request.formVersion,
        revision = request.revision,
      ),
    )

    webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(commandRequest)
      .exchange()
      .expectStatus().isOk

    assertThat(findAll(assessment.assessmentUuid, user)).isEmpty()
  }

  private fun createAssessment(user: UserDetails): AssessmentVersionQueryResult {
    val commandResponse = webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CommandsRequest(
          commands = listOf(
            CreateAssessmentCommand(
              user = user,
              assessmentType = "TEST",
              formVersion = "1.0",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult()
      .responseBody
    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(commandResponse?.commands?.single()?.result).assessmentUuid

    return assertIs<AssessmentVersionQueryResult>(
      webTestClient.post().uri("/query")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          QueriesRequest(
            queries = listOf(
              AssessmentVersionQuery(user = user, assessmentIdentifier = UuidIdentifier(assessmentUuid)),
            ),
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody<QueriesResponse>()
        .returnResult()
        .responseBody
        ?.queries
        ?.single()
        ?.result,
    )
  }

  private fun findAll(assessmentUuid: UUID, user: UserDetails): List<AssessmentDraftResponse> = webTestClient
    .post().uri("/assessment/$assessmentUuid/draft/query")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(AssessmentDraftQueryRequest(user))
    .exchange()
    .expectStatus().isOk
    .expectBodyList(AssessmentDraftResponse::class.java)
    .returnResult()
    .responseBody!!
}
