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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID
import kotlin.test.assertIs

class CreateAssessmentCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it creates an assessment`() {
    val randomCrn = UUID.randomUUID().toString()

    val command = CreateAssessmentCommand(
      user = testUserDetails,
      assessmentType = "TEST",
      identifiers = mapOf(
        IdentifierType.CRN to randomCrn,
      ),
      formVersion = "1",
      properties = mapOf("prop1" to SingleValue("val1")),
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
    assertThat(assessment?.type).isEqualTo("TEST")

    val expectedIdentifier = ExternalIdentifier(randomCrn, IdentifierType.CRN, "TEST")
    assertThat(assessment?.identifiers).hasSize(1)
    assertThat(assessment?.identifiers?.first()?.toIdentifier()).isEqualTo(expectedIdentifier)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentUuid)

    assertThat(eventsForAssessment.size).isEqualTo(2)

    val createdEvent = eventsForAssessment[eventsForAssessment.size - 2].data
    assertIs<AssessmentCreatedEvent>(createdEvent)
    assertThat(createdEvent.formVersion).isEqualTo(command.formVersion)
    assertThat(createdEvent.properties).isEqualTo(command.properties)
    val assignedEvent = eventsForAssessment[eventsForAssessment.size - 1].data
    assertIs<AssignedToUserEvent>(assignedEvent)
  }

  @Test
  fun `it rejects the request if the provided identifier is not unique`() {
    val randomCrn = UUID.randomUUID().toString()

    val command = CreateAssessmentCommand(
      user = testUserDetails,
      assessmentType = "TEST",
      identifiers = mapOf(
        IdentifierType.CRN to randomCrn,
      ),
      formVersion = "1",
    )

    val request = CommandsRequest(
      commands = listOf(command),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val response = webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.userMessage).isEqualTo("The provided identifier already exists")
  }

  @Test
  fun `it ignores the user-provided assessment UUID and assigns a new random UUID`() {
    val requestedAssessmentUuid = UUID.randomUUID()
    val request = """
      {
        "commands": [
          {
            "type": "CreateAssessmentCommand",
            "assessmentType": "TEST",
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
