package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class UpdateFormVersionCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
) : CommandHandler<UpdateFormVersionCommand> {
  override val type = UpdateFormVersionCommand::class
  override fun handle(command: UpdateFormVersionCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = FormVersionUpdatedEvent(version),
      )
    }

    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    return CommandSuccessCommandResult()
  }
}
