package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.RequestableQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.LocalDateTime

@ExtendWith(HmppsAuthApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("postgres", "test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

  @LocalServerPort
  private var port: Int = 0

  protected lateinit var webTestClient: WebTestClient

  protected val testUserDetails =
    UserDetails(id = "FOO_USER", name = "Foo User", authSource = AuthSource.HMPPS_AUTH)

  protected lateinit var testUserDetailsEntity: UserDetailsEntity

  protected val clock: Clock = mockk()

  @BeforeAll
  fun setupWebTestClient() {
    every { clock.now() } answers { LocalDateTime.now() }
    testUserDetailsEntity = userDetailsService.findOrCreate(testUserDetails)
    webTestClient = WebTestClient.bindToServer()
      .baseUrl("http://localhost:$port")
      .build()
  }

  @Autowired
  private lateinit var userDetailsService: UserDetailsService

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  protected fun command(vararg cmd: RequestableCommand) = webTestClient.post().uri("/command")
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
    .bodyValue(CommandsRequest(cmd.toList()))
    .exchange()
    .expectStatus().isEqualTo(HttpStatus.OK)
    .expectBody(CommandsResponse::class.java)
    .returnResult()
    .responseBody!!

  protected fun backdatedCommand(backdateTo: LocalDateTime, vararg cmd: RequestableCommand) = webTestClient.post().uri("/command?backdateTo=$backdateTo")
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
    .bodyValue(CommandsRequest(cmd.toList()))
    .exchange()
    .expectStatus().isEqualTo(HttpStatus.OK)
    .expectBody(CommandsResponse::class.java)
    .returnResult()
    .responseBody!!

  protected fun query(vararg query: RequestableQuery) = webTestClient.post().uri("/query")
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
    .bodyValue(QueriesRequest(query.toList()))
    .exchange()
}
