package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.AddOasysEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertIs

class CommandControllerTest(
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
    val assessmentEntity = AssessmentEntity()
    assessmentRepository.save(assessmentEntity)
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      data = AssessmentVersionAggregate(),
      updatedAt = LocalDateTime.of(2025, 1, 1, 0, 1, 0),
    )
    aggregateRepository.save(aggregateEntity)

    val request = CommandRequest(
      user = User("test-user", "Test User"),
      assessmentUuid = assessmentEntity.uuid,
      commands = listOf(
        UpdateAnswers(mapOf("foo" to listOf("updated")), listOf("bar")),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(1)

    val data = assertIs<AnswersUpdated>(eventsForAssessment[0].data)
    assertThat(data.added).isEqualTo(mapOf("foo" to listOf("updated")))
    assertThat(data.removed).isEqualTo(listOf("bar"))

    val assessmentVersion = aggregateRepository.findLatestByAssessmentAndType(
      assessmentEntity.uuid,
      AssessmentVersionAggregate.aggregateType,
    )
    assertThat(assessmentVersion?.uuid).isEqualTo(aggregateEntity.uuid)
    assertThat(assessmentVersion?.updatedAt?.toLocalDate()).isEqualTo(LocalDate.now())
  }

  @Test
  fun `executes commands for OASys events`() {
    val assessmentEntity = AssessmentEntity()
    assessmentRepository.save(assessmentEntity)
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      data = AssessmentVersionAggregate(),
      updatedAt = LocalDateTime.of(2025, 1, 1, 0, 1, 0),
    )
    aggregateRepository.save(aggregateEntity)

    val request = CommandRequest(
      user = User("test-user", "Test User"),
      assessmentUuid = assessmentEntity.uuid,
      commands = listOf(
        AddOasysEvent(tag = "MERGED"),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(1)

    val data = assertIs<OasysEventAdded>(eventsForAssessment[0].data)
    assertThat(data.tag).isEqualTo("MERGED")

    val assessmentVersion = aggregateRepository.findLatestByAssessmentAndType(assessmentEntity.uuid, AssessmentVersionAggregate.aggregateType)
    assertThat(assessmentVersion?.uuid).isNotEqualTo(aggregateEntity.uuid)
    assertThat(assessmentVersion?.updatedAt).isAfter(aggregateEntity.updatedAt)
  }
}
