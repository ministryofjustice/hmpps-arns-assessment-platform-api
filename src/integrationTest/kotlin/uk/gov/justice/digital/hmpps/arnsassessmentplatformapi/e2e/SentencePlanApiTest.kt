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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.util.UUID

@DisplayName("Create Sentence Plan API Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SentencePlanApiTest : IntegrationTestBase() {
  private lateinit var extractedUuid: UUID

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

    val commandsResponse = webTestClient.post().uri("/command")
      .bodyValue(sentencePlanCommandRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult().responseBody

    assertThat(commandsResponse?.commands?.size).isOne()
    val result = commandsResponse?.commands?.first()?.result
    assertThat(result).isInstanceOf(CreateAssessmentCommandResult::class.java)
    assertThat(result?.success).isTrue()
    extractedUuid = UUID.fromString(result?.message?.substringAfter("UUID ")?.trim())
    assertThat(extractedUuid).isNotNull
  }

  @Test
  fun `create goals`() {
    val createGoalCommandRequest = CommandsRequest(
      commands = listOf(
        CreateCollectionCommand(
          user = UserDetails("test-user", "Test User"),
          name = "GOALS",
          parentCollectionItemUuid = null,
          assessmentUuid = extractedUuid.toReference(),
        ),
      ),
    )

    val commandsResponse = webTestClient.post().uri("/command")
      .bodyValue(createGoalCommandRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult().responseBody

    assertThat(commandsResponse?.commands?.size).isOne()
    val result = commandsResponse?.commands?.first()?.result
    assertThat(result).isInstanceOf(CreateCollectionCommandResult::class.java)
    assertThat(result?.success).isTrue()
  }
}
