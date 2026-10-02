package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.queries.QueryApiHelper.executeSentencePlanQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.util.UUID
import kotlin.test.assertIs

@DisplayName("Create Sentence Plan API Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SentencePlanApiTest : IntegrationTestBase() {
  private lateinit var sentencePlanAssessmentUuid: UUID

  @BeforeAll
  fun `Assert created AAP sentence plan`() {
    val testCrn = UUID.randomUUID().toString()
    val sentencePlanCommandRequest = CommandsRequest(
      commands = listOf(
        CreateAssessmentCommand(
          UserDetails("test-user", "Test User"),
          assessmentType = "SENTENCE_PLAN",
          formVersion = "1.0",
          properties = mapOf(
            "AGREEMENT_STATUS" to SingleValue("DRAFT"),
            "AGREEMENT_DATE" to SingleValue(""),
            "AGREEMENT_NOTES" to SingleValue(""),
            "PLAN_TYPE" to SingleValue("INITIAL"),
          ),
          identifiers = mapOf(IdentifierType.CRN to testCrn),
          flags = listOf("SAN_BETA"),
        ),
      ),
    )

    val createAssessmentCommandResponse = webTestClient.post().uri("/command")
      .bodyValue(sentencePlanCommandRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult().responseBody

    val createAssessmentCommandResult = createAssessmentCommandResponse?.commands?.first()?.result as CreateAssessmentCommandResult
    assertThat(createAssessmentCommandResult.success).isTrue()
    assertThat(createAssessmentCommandResult.assessmentUuid).isNotNull
    sentencePlanAssessmentUuid = createAssessmentCommandResult.assessmentUuid
  }

  @Test
  fun `create goals`() {
    val createGoalCommandRequest = CommandsRequest(
      commands = listOf(
        CreateCollectionCommand(
          user = UserDetails("test-user", "Test User"),
          name = "GOALS",
          parentCollectionItemUuid = null,
          assessmentUuid = sentencePlanAssessmentUuid.toReference(),
        ),
      ),
    )

    val createCollectionCommandResponse = webTestClient.post().uri("/command")
      .bodyValue(createGoalCommandRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult().responseBody

    assertThat(createCollectionCommandResponse?.commands?.first()?.request).isInstanceOf(CreateCollectionCommand::class.java)
    assertThat(createCollectionCommandResponse?.commands?.first()?.result?.success).isTrue()
    val createCollection = assertIs<CreateCollectionCommand>(createCollectionCommandResponse?.commands?.first()?.request)
    assertThat(createCollection.name).isEqualTo("GOALS")
  }

  @Test
  fun `should query sentence plan`() {
    val queryResponse = executeSentencePlanQuery(webTestClient, sentencePlanAssessmentUuid)
    assertThat(queryResponse?.queries?.first()?.request).isInstanceOf(AssessmentVersionQuery::class.java)
    val assessmentVersionQueryResult = assertIs<AssessmentVersionQueryResult>(queryResponse?.queries?.first()?.result)
    assertThat(assessmentVersionQueryResult.assessmentUuid).isEqualTo(sentencePlanAssessmentUuid)
    assertThat(assessmentVersionQueryResult.properties).containsEntry("PLAN_TYPE", SingleValue("INITIAL"))
  }
}
