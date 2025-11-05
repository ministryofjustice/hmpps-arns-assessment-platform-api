package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import java.util.UUID
import kotlin.test.assertIs

class UpdateAssessmentStatusCommandHandlerTest {
  val assessmentService: AssessmentService = mockk()
  val eventBus: EventBus = mockk()

  val handler = UpdateAssessmentPropertiesCommandHandler(
      assessmentService = assessmentService,
      eventBus = eventBus,
      eventService = eventService,
  )

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(UpdateAssessmentPropertiesCommand::class)
  }

  @Test
  fun `it handles the UpdateAssessmentStatus command`() {
    val command = UpdateAssessmentPropertiesCommand(
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
    val eventData = assertIs<AssessmentPropertiesUpdatedEvent>(event.captured.data)
    assertThat(eventData.status).isEqualTo(command.status)
  }
}
