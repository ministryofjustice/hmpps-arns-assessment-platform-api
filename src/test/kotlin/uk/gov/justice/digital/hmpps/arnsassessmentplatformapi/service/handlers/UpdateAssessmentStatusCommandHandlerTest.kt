package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAssessmentStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentStatusUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventBus
import java.util.UUID
import kotlin.test.assertIs

class UpdateAssessmentStatusCommandHandlerTest {
  val assessmentService: AssessmentService = mockk()
  val eventBus: EventBus = mockk()

  val handler = UpdateAssessmentStatusCommandHandler(
    assessmentService = assessmentService,
    eventBus = eventBus,
  )

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(UpdateAssessmentStatus::class)
  }

  @Test
  fun `it handles the UpdateAssessmentStatus command`() {
    val command = UpdateAssessmentStatus(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
      status = "COMPLETE",
    )

    val assessment = AssessmentEntity(uuid = command.assessmentUuid)
    every { assessmentService.findByUuid(command.assessmentUuid) } returns assessment

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.assessment.uuid).isEqualTo(command.assessmentUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    val eventData = assertIs<AssessmentStatusUpdated>(event.captured.data)
    assertThat(eventData.status).isEqualTo(command.status)
  }
}
