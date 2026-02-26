package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import kotlin.test.assertIs

class GroupCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val aggregateRepository: AggregateRepository,
) : IntegrationTestBase() {
  @Autowired
  private lateinit var eventRepository: EventRepository

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it groups events together`() {
    val assessmentEntity = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.parse("2025-01-01T12:35:00"))
    assessmentRepository.save(assessmentEntity)
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
      data = AssessmentAggregate().apply {
        formVersion = "1"
        answers.put("foo", SingleValue("bar"))
      },
    )
    aggregateRepository.save(aggregateEntity)

    eventRepository.saveAll(
      listOf(
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          data = AssessmentCreatedEvent(
            formVersion = "1",
            properties = emptyMap(),
          ),
        ),
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf("foo" to SingleValue("bar")),
            removed = listOf(),
          ),
        ),
      ),
    )

    val updateCommand = GroupCommand(
      user = testUserDetails,
      assessmentUuid = assessmentEntity.uuid.toReference(),
      commands = listOf(
        UpdateFormVersionCommand(
          user = testUserDetails,
          assessmentUuid = assessmentEntity.uuid.toReference(),
          version = "2",
        ),
        UpdateAssessmentAnswersCommand(
          user = testUserDetails,
          assessmentUuid = assessmentEntity.uuid.toReference(),
          added = mapOf("bar" to SingleValue("baz")),
          removed = listOf("foo"),
        ),
        UpdateAssessmentPropertiesCommand(
          user = testUserDetails,
          assessmentUuid = assessmentEntity.uuid.toReference(),
          added = mapOf("foo" to SingleValue("baz")),
          removed = listOf(),
        ),
      ),
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
    val result = assertIs<GroupCommandResult>(response?.commands[0]?.result)

    assertThat(result.commands).hasSize(3)
    assertThat(result.commands[0].request).isEqualTo(updateCommand.commands[0])
    assertIs<CommandSuccessCommandResult>(result.commands[0].result)
    assertThat(result.commands[1].request).isEqualTo(updateCommand.commands[1])
    assertIs<CommandSuccessCommandResult>(result.commands[1].result)
    assertThat(result.commands[2].request).isEqualTo(updateCommand.commands[2])
    assertIs<CommandSuccessCommandResult>(result.commands[2].result)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid).sortedBy { it.createdAt }

    assertThat(eventsForAssessment.size).isEqualTo(6)
    assertThat(eventsForAssessment[2].data).isInstanceOf(GroupEvent::class.java)
    assertNull(eventsForAssessment[2].parent)
    assertThat(eventsForAssessment[3].data).isInstanceOf(FormVersionUpdatedEvent::class.java)
    assertThat(eventsForAssessment[3].parent?.uuid).isEqualTo(eventsForAssessment[2].uuid)
    assertThat(eventsForAssessment[4].data).isInstanceOf(AssessmentAnswersUpdatedEvent::class.java)
    assertThat(eventsForAssessment[4].parent?.uuid).isEqualTo(eventsForAssessment[2].uuid)
    assertThat(eventsForAssessment[5].data).isInstanceOf(AssessmentPropertiesUpdatedEvent::class.java)
    assertThat(eventsForAssessment[5].parent?.uuid).isEqualTo(eventsForAssessment[2].uuid)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentAggregate::class.simpleName!!,
      Clock.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentAggregate>(aggregate?.data)
    assertThat(data.formVersion).isEqualTo("2")
    assertThat(data.answers).isEqualTo(mapOf("bar" to SingleValue("baz")))
    assertThat(data.properties).isEqualTo(mapOf("foo" to SingleValue("baz")))
  }
}
