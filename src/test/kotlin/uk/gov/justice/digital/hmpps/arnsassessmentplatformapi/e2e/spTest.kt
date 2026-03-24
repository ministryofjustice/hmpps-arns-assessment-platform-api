package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.e2e

import org.springframework.http.MediaType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.test.web.reactive.server.WebTestClient
import org.junit.jupiter.api.BeforeAll
import java.util.Base64
import org.assertj.core.api.Assertions.assertThat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class spTest {
  protected lateinit var webTestClient: WebTestClient

  protected lateinit var authTestClient: WebTestClient

  @BeforeAll
  fun setupWebTestClient() {
    authTestClient = WebTestClient.bindToServer()
      .baseUrl("https://sign-in-dev.hmpps.service.justice.gov.uk")
      .build()

//    webTestClient = WebTestClient.bindToServer()
//      .baseUrl("https://arns-assessment-platform-api-dev.hmpps.service.justice.gov.uk")
//      .build()
  }

  @Test
  fun whenGetAuthToken_thenExpectStatus() {
    val clientId = System.getenv("AAP_CLIENT_ID") ?: throw IllegalStateException("CLIENT_ID missing!")
    val clientSecret = System.getenv("AAP_CLIENT_SECRET") ?: "default_secret"
    var secret = "$clientId:$clientSecret".toByteArray()
    val encodedAuth = Base64.getEncoder().encodeToString(secret)

    val body = authTestClient.post()
      .uri("/auth/oauth/token")
      .header("Authorization", "Basic $encodedAuth")
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(TokenResponse::class.java)
      .returnResult()
      .responseBody

    val token = body?.access_token

    assertThat(token).isNotBlank()

//    webClient.post()
//      .uri("/query")
//      .bodyValue({
//        data: {
//        queries: [
//        {
//          type: 'AssessmentVersionQuery',
//          user: { id: 'test-user', name: 'Test User' },
//          assessmentIdentifier: { type: 'UUID', uuid: 'c62c6cde-5216-457d-8007-1e8370a7908f' },
//        },
//        ],
//      },
//      })
//      .exchange()
//      .expectStatus()
//      .isOk()
  }
}