package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.util.UUID

@DisplayName("Create Assessment API Tests")
class AapAssessmentApiTest : IntegrationTestBase() {

  @Test
  fun `create assessment`() {
    val testCrn = UUID.randomUUID().toString()
    val assessmentCommandRequest = CommandsRequest(
      commands = listOf(
        CreateAssessmentCommand(
          UserDetails("test-user", "Test User"),
          assessmentType = "TEST",
          formVersion = "1.0",
          identifiers = mapOf(IdentifierType.CRN to testCrn),
          flags = listOf("SAN_BETA"),
        ),
      ),
    )

    val commandsResponse = webTestClient.post().uri("/command")
      .bodyValue(assessmentCommandRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult().responseBody

    assertThat(commandsResponse?.commands?.size).isOne()
    val result = commandsResponse?.commands?.first()?.result
    assertThat(result).isInstanceOf(CreateAssessmentCommandResult::class.java)
    assertThat(result?.success).isTrue()
  }
}
