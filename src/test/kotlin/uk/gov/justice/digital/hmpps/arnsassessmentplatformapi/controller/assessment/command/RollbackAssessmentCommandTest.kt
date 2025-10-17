package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import kotlin.test.assertIs

class RollbackAssessmentCommandTest(
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
  fun `it creates a rollback for a point in time`() {
    val assessmentEntity = AssessmentEntity(createdAt = LocalDateTime.parse("2025-01-01T12:00:00"))
    assessmentRepository.save(assessmentEntity)
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
      data = AssessmentVersionAggregate(),
    )
    aggregateRepository.save(aggregateEntity)

    val user = User("FOO_USER", "Foo User")

    eventRepository.saveAll(
      listOf(
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          data = AssessmentCreatedEvent(),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          data = AnswersUpdatedEvent(
            added = mapOf(
              "foo" to listOf("bar"),
            ),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T13:45:00"),
          data = AnswersUpdatedEvent(
            added = mapOf(
              "foo" to listOf("baz"),
            ),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-02T09:30:00"),
          data = AnswersUpdatedEvent(
            added = mapOf(
              "bar" to listOf("foo"),
            ),
            removed = emptyList(),
          ),
        ),
      ),
    )

    val request = CommandsRequest(

      commands = listOf(
        RollbackAnswersCommand(
          user = User("test-user", "Test User"),
          assessmentUuid = assessmentEntity.uuid,
          pointInTime = LocalDateTime.parse("2025-01-01T13:00:00"),
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
    assertIs<CommandSuccessCommandResult>(response?.commands[0]?.result)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(5)
    assertThat(eventsForAssessment.last().data).isInstanceOf(AnswersRolledBackEvent::class.java)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentVersionAggregate::class.simpleName!!,
      LocalDateTime.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentVersionAggregate>(aggregate?.data)
    assertThat(data.getAnswers()["foo"]).isEqualTo(listOf("bar"))
    assertThat(data.getAnswers()["bar"]).isNull()

    val secondRequest = CommandsRequest(

      commands = listOf(
        RollbackAnswersCommand(
          user = User("test-user", "Test User"),
          assessmentUuid = assessmentEntity.uuid,
          pointInTime = LocalDateTime.parse("2025-01-02T10:00:00"),
        ),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(secondRequest)
      .exchange()
      .expectStatus().isOk

    val eventsAfterSecondRollback = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsAfterSecondRollback.size).isEqualTo(6)
    assertThat(eventsAfterSecondRollback.last().data).isInstanceOf(AnswersRolledBackEvent::class.java)

    val aggregateAfterSecondUpdate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentVersionAggregate::class.simpleName!!,
      LocalDateTime.now(),
    )

    assertThat(aggregateAfterSecondUpdate).isNotNull
    val dataAfterSecondUpdate = assertIs<AssessmentVersionAggregate>(aggregateAfterSecondUpdate?.data)
    assertThat(dataAfterSecondUpdate.getAnswers()["foo"]).isEqualTo(listOf("baz"))
    assertThat(dataAfterSecondUpdate.getAnswers()["bar"]).isEqualTo(listOf("foo"))
  }
}
