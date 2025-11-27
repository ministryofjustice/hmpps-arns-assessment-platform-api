package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.FormVersionUpdatedCommandResult
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
  @param:Lazy private val commandBus: CommandBus,
) : CommandHandler<UpdateFormVersionCommand> {
  override val type = UpdateFormVersionCommand::class
  override fun handle(command: UpdateFormVersionCommand): FormVersionUpdatedCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = FormVersionUpdatedEvent(version, timeline),
      )
    }
    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    eventService.setParentEvent(event)
    val commandsResponse = commandBus.dispatch(command.commands)
    eventService.clearParentEvent()

    return FormVersionUpdatedCommandResult(
      commands = commandsResponse.commands,
    )
  }
}
