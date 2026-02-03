package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class RollbackCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<RollbackCommand> {
  override val type = RollbackCommand::class
  override fun handle(command: RollbackCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid),
        data = AssessmentRolledBackEvent(
          rolledBackTo = command.pointInTime,
        ),
      )
    }

    services.eventBus.handle(event).run(services.state::persist)
    services.event.save(event)
    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "rolledBackTo" to command.pointInTime,
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
