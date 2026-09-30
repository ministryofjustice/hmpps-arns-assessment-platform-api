package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.queries

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import java.util.UUID

class QueryApiHelper {

  companion object {
    val assessmentId: String = System.getenv("AAP_API_ASSESSMENT") ?: "caff2f14-a083-41f0-8d26-638b30177511"
    val sentencePlanId: String = System.getenv("AAP_API_SENTENCE_PLAN") ?: "fb56a1f9-85b9-40e7-8be4-14134dfcfed1"

    fun executeAssessmentQuery(webTestClient: WebTestClient, assessmentIdentifier: String = assessmentId): QueriesResponse? = getQueryResponse(webTestClient, assessmentIdentifier)

    fun executeSentencePlanQuery(webTestClient: WebTestClient, sentencePlanIdentifier: String = sentencePlanId): QueriesResponse? = getQueryResponse(webTestClient, sentencePlanIdentifier)

    fun getQueryResponse(webTestClient: WebTestClient, identifier: String): QueriesResponse? {
      val testUserDetails = UserDetails(id = "test-user", name = "Test User")
      val assessmentVersionQuery = QueriesRequest(
        queries = listOf(
          AssessmentVersionQuery(
            user = testUserDetails,
            assessmentIdentifier = UuidIdentifier(UUID.fromString((identifier))),
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

      return queryResponse
    }
  }
}
