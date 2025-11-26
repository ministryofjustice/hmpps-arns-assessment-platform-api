package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
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
    val command = CreateAssessmentCommand(
      user = User("test-user", "Test User"),
      formVersion = "1",
      properties = mapOf("prop1" to listOf("val1")),
    )

    val request = CommandsRequest(
      commands = listOf(command),
    )

    val response = webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.commands).hasSize(1)
    assertThat(response?.commands[0]?.request).isEqualTo(request.commands[0])
    val result = assertIs<CreateAssessmentCommandResult>(response?.commands[0]?.result)

    val assessmentUuid = requireNotNull(result.assessmentUuid) { "An assessmentUuid should be present on the response" }

    val assessment = assessmentRepository.findByUuid(assessmentUuid)

    assertThat(assessment).isNotNull

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentUuid)

    assertThat(eventsForAssessment.size).isEqualTo(1)

    val event = eventsForAssessment.last().data
    assertIs<AssessmentCreatedEvent>(event)

    assertThat(event.formVersion).isEqualTo(command.formVersion)
    assertThat(event.properties).isEqualTo(command.properties)
  }

  @Test
  fun `it ignores the user-provided assessment UUID and assigns a new random UUID`() {
    val requestedAssessmentUuid = UUID.randomUUID()
    val request = """
      {
        "commands": [
          {
            "type": "CreateAssessmentCommand",
            "formVersion": "1",
            "properties": {},
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
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    val result = assertIs<CreateAssessmentCommandResult>(response?.commands[0]?.result)
    val actualAssessmentUuid = requireNotNull(result.assessmentUuid) { "An assessmentUuid should be present on the response" }

    assertNull(assessmentRepository.findByUuid(requestedAssessmentUuid))
    assertThat(assessmentRepository.findByUuid(actualAssessmentUuid)).isNotNull
  }
}
