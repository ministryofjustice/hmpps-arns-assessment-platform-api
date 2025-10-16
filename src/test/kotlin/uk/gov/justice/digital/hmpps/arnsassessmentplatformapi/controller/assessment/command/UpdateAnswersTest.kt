package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import kotlin.test.assertIs

class UpdateAnswersTest(
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
  fun `it updates answers`() {
    val assessmentEntity = AssessmentEntity(createdAt = LocalDateTime.parse("2025-01-01T12:35:00"))
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
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          data = AssessmentCreated(),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          data = AnswersUpdated(
            added = mapOf(
              "foo" to listOf("foo_value"),
              "bar" to listOf("bar_value"),
            ),
            removed = emptyList(),
          ),
        ),
      ),
    )

    val request = CommandsRequest(

      commands = listOf(
        UpdateAnswers(
          user = User("test-user", "Test User"),
          assessmentUuid = assessmentEntity.uuid,
          added = mapOf("foo" to listOf("updated_foo_value"), "baz" to listOf("baz_value")),
          removed = listOf("bar"),
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
    assertIs<CommandSuccessResult>(response?.commands[0]?.result)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(3)
    assertThat(eventsForAssessment.last().data).isInstanceOf(AnswersUpdated::class.java)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentVersionAggregate.aggregateType,
      LocalDateTime.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentVersionAggregate>(aggregate?.data)
    assertThat(data.getAnswers()["foo"]).isEqualTo(listOf("updated_foo_value"))
    assertThat(data.getAnswers()["bar"]).isNull()
    assertThat(data.getAnswers()["baz"]).isEqualTo(listOf("baz_value"))
  }
}
