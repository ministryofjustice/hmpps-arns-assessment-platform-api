package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent
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

class RollBackAssessmentAnswersCommandTest(
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
    val assessmentEntity = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.parse("2025-01-01T12:00:00"))
    assessmentRepository.save(assessmentEntity)
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
      data = AssessmentAggregate().apply {
        formVersion = "1"
      },
    )
    aggregateRepository.save(aggregateEntity)

    eventRepository.saveAll(
      listOf(
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
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
            added = mapOf(
              "foo" to SingleValue("bar"),
            ),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T13:45:00"),
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf(
              "foo" to SingleValue("baz"),
            ),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-02T09:30:00"),
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf(
              "bar" to SingleValue("foo"),
            ),
            removed = emptyList(),
          ),
        ),
      ),
    )

    val request = CommandsRequest(

      commands = listOf(
        RollbackCommand(
          user = testUserDetails,
          assessmentUuid = assessmentEntity.uuid.toReference(),
          pointInTime = LocalDateTime.parse("2025-01-01T13:00:00"),
        ),
      ),
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
    assertIs<CommandSuccessCommandResult>(response?.commands[0]?.result)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(5)
    assertThat(eventsForAssessment.last().data).isInstanceOf(AssessmentRolledBackEvent::class.java)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentAggregate::class.simpleName!!,
      clock.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentAggregate>(aggregate?.data)
    assertThat(data.answers["foo"]).isEqualTo(SingleValue("bar"))
    assertThat(data.answers["bar"]).isNull()

    val secondRequest = CommandsRequest(

      commands = listOf(
        RollbackCommand(
          user = testUserDetails,
          assessmentUuid = assessmentEntity.uuid.toReference(),
          pointInTime = LocalDateTime.parse("2025-01-02T10:00:00"),
        ),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(secondRequest)
      .exchange()
      .expectStatus().isOk

    val eventsAfterSecondRollback = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsAfterSecondRollback.size).isEqualTo(6)
    assertThat(eventsAfterSecondRollback.last().data).isInstanceOf(AssessmentRolledBackEvent::class.java)

    val aggregateAfterSecondUpdate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentAggregate::class.simpleName!!,
      clock.now(),
    )

    assertThat(aggregateAfterSecondUpdate).isNotNull
    val dataAfterSecondUpdate = assertIs<AssessmentAggregate>(aggregateAfterSecondUpdate?.data)
    assertThat(dataAfterSecondUpdate.answers["foo"]).isEqualTo(SingleValue("baz"))
    assertThat(dataAfterSecondUpdate.answers["bar"]).isEqualTo(SingleValue("foo"))
  }
}
