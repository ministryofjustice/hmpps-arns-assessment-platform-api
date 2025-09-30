package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated
import java.time.LocalDateTime
import kotlin.test.assertIs

class UpdateFormVersionCommandTest(
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
  fun `it executes commands for assessments`() {
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
          data = FormVersionUpdated(
            version = "old_form_version",
          ),
        ),
      ),
    )

    val request = CommandRequest(
      user = User("test-user", "Test User"),
      assessmentUuid = assessmentEntity.uuid,
      commands = listOf(
        UpdateFormVersion("updated_form_version"),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(3)
    assertThat(eventsForAssessment.last().data).isInstanceOf(FormVersionUpdated::class.java)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentVersionAggregate.aggregateType,
      LocalDateTime.now(),
    )

    assertThat(aggregate).isNotNull
    val data = assertIs<AssessmentVersionAggregate>(aggregate?.data)
    assertThat(data.getFormVersion()).isEqualTo("updated_form_version")
  }
}
