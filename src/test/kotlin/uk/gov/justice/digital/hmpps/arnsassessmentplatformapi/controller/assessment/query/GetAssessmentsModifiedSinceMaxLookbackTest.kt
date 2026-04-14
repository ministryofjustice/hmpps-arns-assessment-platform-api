package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsModifiedSinceQuery
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["app.query.max-lookback-days=1"])
class GetAssessmentsModifiedSinceMaxLookbackTest : IntegrationTestBase() {

  @Test
  fun `it rejects queries where since exceeds max lookback`() {
    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsModifiedSinceQuery(
          user = testUserDetails,
          assessmentType = "SENTENCE_PLAN",
          since = LocalDateTime.now().minusDays(30),
        ),
      ),
    )

    val response = webTestClient.post().uri("/query")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.status).isEqualTo(400)
    assertThat(response?.developerMessage).contains("older than 1 day(s)")
  }

}
