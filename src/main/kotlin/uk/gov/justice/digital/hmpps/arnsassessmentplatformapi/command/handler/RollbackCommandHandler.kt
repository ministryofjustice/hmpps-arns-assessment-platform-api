package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class RollbackCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
) : CommandHandler<RollbackCommand> {
  override val type = RollbackCommand::class
  override fun handle(command: RollbackCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = command.user,
        assessment = assessmentService.findBy(assessmentUuid),
        data = AssessmentRolledBackEvent(
          rolledBackTo = command.pointInTime,
          timeline = timeline,
        ),
      )
    }

    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    return CommandSuccessCommandResult()
  }
}
