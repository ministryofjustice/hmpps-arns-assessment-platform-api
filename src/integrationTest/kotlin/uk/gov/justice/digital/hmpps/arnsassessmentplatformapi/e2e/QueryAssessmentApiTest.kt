package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import java.util.UUID

@DisplayName("AAP API Tests")
class QueryAssessmentApiTest : IntegrationTestBase() {

  @Test
  fun `query assessment`() {
    val testUserDetails = UserDetails(id = "test-user", name = "Test User")
    val assessmentVersionQuery = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(UUID.fromString((assessmentId)))
        ),
      ),
    )

    val queryResponse = webTestClient.post().uri("/query")
      .bodyValue(assessmentVersionQuery)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<QueriesResponse>()
      .returnResult().responseBody

    assertThat(queryResponse?.queries).isNotEmpty()
  }
}
