package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.queries

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.QueryAssessmentApiTest.Companion.assessmentId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e.SentencePlanApiTest.Companion.sentencePlanId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import java.util.UUID

object QueryApiHelper {
  fun executeAssessmentQuery(webTestClient: WebTestClient, assessmentIdentifier: UUID = assessmentId): QueriesResponse? = getQueryResponse(webTestClient, assessmentIdentifier)

  fun executeSentencePlanQuery(webTestClient: WebTestClient, sentencePlanIdentifier: UUID = sentencePlanId): QueriesResponse? = getQueryResponse(webTestClient, sentencePlanIdentifier)

  fun getQueryResponse(webTestClient: WebTestClient, identifier: UUID): QueriesResponse? {
    val testUserDetails = UserDetails(id = "test-user", name = "Test User")
    val assessmentVersionQuery = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(identifier),
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
