package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import kotlin.test.assertIs

class UpdateFlagsCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val aggregateRepository: AggregateRepository,
  @Autowired
  val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @Test
  fun `it replaces the flags on an assessment`() {
    val assessmentEntity = givenAnAssessmentWithFlags(listOf("OLD_FLAG"))

    val updateCommand = UpdateFlagsCommand(
      user = testUserDetails,
      assessmentUuid = assessmentEntity.uuid.toReference(),
      flags = listOf("SAN_BETA", "ANOTHER_FLAG"),
    )

    val request = CommandsRequest(
      commands = listOf(updateCommand),
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
    assertThat(response?.commands[0]?.request).isEqualTo(updateCommand)
    assertIs<CommandSuccessCommandResult>(response?.commands[0]?.result)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuidOrderById(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(2)
    val flagsUpdatedEvent = assertIs<AssessmentFlagsUpdatedEvent>(eventsForAssessment.last().data)
    assertThat(flagsUpdatedEvent.flags).isEqualTo(listOf("SAN_BETA", "ANOTHER_FLAG"))

    val aggregate = aggregateRepository.findTopByAssessmentUuidAndDataTypeAndEventsToLessThanEqualOrderByPositionDesc(
      assessmentEntity.uuid,
      AssessmentAggregate::class.simpleName!!,
      clock.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentAggregate>(aggregate?.data)
    assertThat(data.flags).isEqualTo(listOf("SAN_BETA", "ANOTHER_FLAG"))
  }

  @Test
  fun `it accepts the command as raw JSON with a bare string assessment UUID`() {
    val assessmentEntity = givenAnAssessmentWithFlags(listOf("OLD_FLAG"))

    val request = """
      {
        "commands": [
          {
            "type": "UpdateFlagsCommand",
            "user": {
              "id": "${testUserDetails.id}",
              "name": "${testUserDetails.name}",
              "authSource": "${testUserDetails.authSource}"
            },
            "assessmentUuid": "${assessmentEntity.uuid}",
            "flags": ["SAN_BETA"]
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

    assertThat(response?.commands).hasSize(1)
    val command = assertIs<UpdateFlagsCommand>(response?.commands[0]?.request)
    assertThat(command.assessmentUuid.rawValue).isEqualTo(assessmentEntity.uuid.toString())
    assertThat(command.flags).isEqualTo(listOf("SAN_BETA"))
    assertIs<CommandSuccessCommandResult>(response?.commands[0]?.result)
  }

  @Test
  fun `it returns the updated flags from an assessment version query`() {
    val assessmentEntity = givenAnAssessmentWithFlags(listOf("OLD_FLAG"))

    val commandRequest = CommandsRequest(
      commands = listOf(
        UpdateFlagsCommand(
          user = testUserDetails,
          assessmentUuid = assessmentEntity.uuid.toReference(),
          flags = listOf("SAN_BETA"),
        ),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(commandRequest)
      .exchange()
      .expectStatus().isOk

    val queryRequest = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessmentEntity.uuid),
        ),
      ),
    )

    val response = webTestClient.post().uri("/query")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(queryRequest)
      .exchange()
      .expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.queries).hasSize(1)
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    assertThat(result.flags).isEqualTo(listOf("SAN_BETA"))
  }

  private fun givenAnAssessmentWithFlags(flags: List<String>): AssessmentEntity {
    val assessmentEntity = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.parse("2025-01-01T12:00:00"))
    assessmentRepository.save(assessmentEntity)

    aggregateRepository.save(
      AggregateEntity(
        assessment = assessmentEntity,
        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
        position = 0,
        data = AssessmentAggregate().apply {
          formVersion = "1"
          this.flags.addAll(flags)
        },
      ),
    )

    eventRepository.save(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessmentEntity,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = emptyMap(),
          flags = flags,
        ),
        position = 0,
      ),
    )

    return assessmentEntity
  }
}
