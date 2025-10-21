package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class UpdateAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateAnswersCommand> {
  override val type = UpdateAnswersCommand::class
  override fun handle(command: UpdateAnswersCommand): CommandSuccessCommandResult {
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessmentService.findByUuid(assessmentUuid),
          data = AnswersUpdatedEvent(
            added = added,
            removed = removed,
          ),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
