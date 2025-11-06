package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class UpdateAssessmentPropertiesCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
) : CommandHandler<UpdateAssessmentPropertiesCommand> {
  override val type = UpdateAssessmentPropertiesCommand::class
  override fun handle(command: UpdateAssessmentPropertiesCommand): CommandSuccessCommandResult {
    with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = AssessmentPropertiesUpdatedEvent(added, removed),
      )
    }
      .run(eventService::save)
      .run(eventBus::handle)
      .run(stateService::persist)

    return CommandSuccessCommandResult()
  }
}
