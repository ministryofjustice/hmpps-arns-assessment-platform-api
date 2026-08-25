package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class UpdateFlagsCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UpdateFlagsCommand> {
  override val type = UpdateFlagsCommand::class

  override fun handle(command: UpdateFlagsCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.persistenceContext.findUserDetails(user),
        assessment = services.persistenceContext.findAssessment(assessmentUuid.value),
        data = AssessmentFlagsUpdatedEvent(flags),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return CommandSuccessCommandResult()
  }
}
