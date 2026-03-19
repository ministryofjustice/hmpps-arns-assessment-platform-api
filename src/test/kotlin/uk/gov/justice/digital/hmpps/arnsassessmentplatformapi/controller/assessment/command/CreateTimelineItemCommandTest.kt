package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.time.LocalDateTime
import kotlin.test.assertIs

class CreateTimelineItemCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
) : IntegrationTestBase() {
  @Autowired
  private lateinit var timelineRepository: TimelineRepository

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it creates a timeline item`() {
    val assessmentEntity = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.parse("2025-01-01T12:35:00"))
    assessmentRepository.save(assessmentEntity)

    val command = CreateTimelineItemCommand(
      user = testUserDetails,
      assessmentUuid = assessmentEntity.uuid.toReference(),
      timestamp = LocalDateTime.parse("2025-01-01T12:36:00"),
      timeline = Timeline(
        type = "SIGNIFICANT_EVENT_A",
        data = mapOf("foo" to "bar"),
      ),
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
    assertThat(response?.commands?.first()?.request).isEqualTo(request.commands.first())
    assertIs<CommandSuccessCommandResult>(response?.commands?.first()?.result)

    val timelineForAssessment = timelineRepository.findByAssessmentUuid(assessmentEntity.uuid)

    assertThat(timelineForAssessment.size).isEqualTo(1)

    val timeline = timelineForAssessment.last()

    assertThat(timeline.data).isEmpty()
    assertThat(timeline.eventType).isNull()
    assertThat(timeline.customType).isEqualTo("SIGNIFICANT_EVENT_A")
    assertThat(timeline.customData).containsExactlyEntriesOf(mapOf("foo" to "bar"))
    assertThat(timeline.createdAt).isEqualTo(command.timestamp)
    assertThat(timeline.assessment.uuid).isEqualTo(assessmentEntity.uuid)
    assertThat(timeline.user.uuid).isEqualTo(testUserDetailsEntity.uuid)
  }
}
