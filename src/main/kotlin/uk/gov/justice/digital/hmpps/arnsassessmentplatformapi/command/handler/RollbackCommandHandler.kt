package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventProto

class RollbackCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<RollbackCommand> {
  override val type = RollbackCommand::class
  override fun handle(command: RollbackCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventProto(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid.value),
        data = AssessmentRolledBackEvent(
          rolledBackTo = command.pointInTime,
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return CommandSuccessCommandResult()
  }
}
