package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentResult
import java.util.UUID
import kotlin.test.assertIs

class CreateAssessmentCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val eventRepository: EventRepository,
) : IntegrationTestBase() {

  val user = User("FOO_USER", "Foo User")

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it creates an assessment`() {
    val request = CommandsRequest(
      commands = listOf(
        CreateAssessment(
          user = User("test-user", "Test User"),
        ),
      ),
    )

    val response = webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.commands).hasSize(1)
    assertThat(response?.commands[0]?.request).isEqualTo(request.commands[0])
    val result = assertIs<CreateAssessmentResult>(response?.commands[0]?.result)

    val assessmentUuid = requireNotNull(result.assessmentUuid) { "An assessmentUuid should be present on the response" }

    val assessment = assessmentRepository.findByUuid(assessmentUuid)

    assertThat(assessment).isNotNull

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentUuid)

    assertThat(eventsForAssessment.size).isEqualTo(1)
    assertIs<AssessmentCreated>(eventsForAssessment.last().data)
  }

  @Test
  fun `it ignores the user-provided assessment UUID and assigns a new random UUID`() {
    val requestedAssessmentUuid = UUID.randomUUID()
    val request = """
      {
        "commands": [
          {
            "type": "CREATE_ASSESSMENT",
            "user": {
              "id": "test-user",
              "name": "Test User"
            },
            "assessmentUuid": "$requestedAssessmentUuid"
          }
        ]
      }
    """.trimIndent()

    val response = webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    val result = assertIs<CreateAssessmentResult>(response?.commands[0]?.result)
    val actualAssessmentUuid = requireNotNull(result.assessmentUuid) { "An assessmentUuid should be present on the response" }

    assertNull(assessmentRepository.findByUuid(requestedAssessmentUuid))
    assertThat(assessmentRepository.findByUuid(actualAssessmentUuid)).isNotNull
  }
}
