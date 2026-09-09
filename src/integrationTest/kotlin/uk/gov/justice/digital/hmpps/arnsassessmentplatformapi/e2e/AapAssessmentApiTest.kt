package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.dto.AssessmentRequestDto
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.dto.Command
import java.util.UUID

@DisplayName("AAP API Tests")
class AapAssessmentApiTest : IntegrationTestBase() {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Test
  fun `create assessment`() {
    val testUserDetails = UserDetails(id = "test-user", name = "Test User")
    val testCrn = UUID.randomUUID().toString()
    val command = Command(
      type = "CreateAssessmentCommand",
      assessmentType = "Test",
      formVersion = "1.0",
      properties = emptyMap(),
      user = testUserDetails,
      CRN = testCrn,
      flags = listOf("SAN_BETA")
    )
    val assessmentRequestDto = AssessmentRequestDto(commands = listOf(command))

    val commandResponse = webTestClient.post().uri("/command")
      .bodyValue(assessmentRequestDto)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandResponse>()
      .returnResult().responseBody

    assertThat(commandResponse?.result?.success).isTrue()
  }
}
