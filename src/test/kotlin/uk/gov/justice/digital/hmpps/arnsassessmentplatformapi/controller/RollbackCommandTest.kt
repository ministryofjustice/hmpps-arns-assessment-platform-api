package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RollbackAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import java.time.LocalDateTime
import kotlin.test.assertIs

class RollbackCommandTest(
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
  fun `executes commands for assessments`() {
    val assessmentEntity = AssessmentEntity(createdAt = LocalDateTime.of(2025, 1, 1, 12, 35, 0))
    assessmentRepository.save(assessmentEntity)
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      updatedAt = LocalDateTime.of(2025, 1, 1, 12, 0, 0),
      eventsFrom = LocalDateTime.of(2025, 1, 1, 12, 0, 0),
      eventsTo = LocalDateTime.of(2025, 1, 1, 12, 0, 0),
      data = AssessmentVersionAggregate(),
    )
    aggregateRepository.save(aggregateEntity)

    val user = User("FOO_USER", "Foo User")

    eventRepository.saveAll(
      listOf(
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.of(2025, 1, 1, 12, 30, 0),
          data = AssessmentCreated(),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.of(2025, 1, 1, 12, 30, 0),
          data = AnswersUpdated(
            added = mapOf(
              "foo" to listOf("bar"),
            ),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.of(2025, 1, 1, 13, 45, 0),
          data = AnswersUpdated(
            added = mapOf(
              "foo" to listOf("baz"),
            ),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.of(2025, 1, 2, 9, 30, 0),
          data = AnswersUpdated(
            added = mapOf(
              "bar" to listOf("foo"),
            ),
            removed = emptyList(),
          ),
        ),
      ),
    )

    val request = CommandRequest(
      user = User("test-user", "Test User"),
      assessmentUuid = assessmentEntity.uuid,
      commands = listOf(
        RollbackAssessment(
          dateAndTime = LocalDateTime.of(2025, 1, 1, 13, 0, 0),
        ),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(5)
    assertThat(eventsForAssessment.last().data).isInstanceOf(AnswersRolledBack::class.java)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentVersionAggregate.aggregateType,
      LocalDateTime.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentVersionAggregate>(aggregate?.data)
    assertThat(data.getAnswers()["foo"]).isEqualTo(listOf("bar"))
    assertThat(data.getAnswers()["bar"]).isNull()

    val secondRequest = CommandRequest(
      user = User("test-user", "Test User"),
      assessmentUuid = assessmentEntity.uuid,
      commands = listOf(
        RollbackAssessment(
          dateAndTime = LocalDateTime.of(2025, 1, 2, 10, 0, 0),
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
    assertThat(eventsAfterSecondRollback.last().data).isInstanceOf(AnswersRolledBack::class.java)

    val aggregateAfterSecondUpdate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentVersionAggregate.aggregateType,
      LocalDateTime.now(),
    )

    assertThat(aggregateAfterSecondUpdate).isNotNull
    val dataAfterSecondUpdate = assertIs<AssessmentVersionAggregate>(aggregateAfterSecondUpdate?.data)
    assertThat(dataAfterSecondUpdate.getAnswers()["foo"]).isEqualTo(listOf("baz"))
    assertThat(dataAfterSecondUpdate.getAnswers()["bar"]).isEqualTo(listOf("foo"))
  }
}
