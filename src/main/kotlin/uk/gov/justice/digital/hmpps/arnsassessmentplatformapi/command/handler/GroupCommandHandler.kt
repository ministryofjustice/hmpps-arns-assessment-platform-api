package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class GroupCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<GroupCommand> {
  override val type = GroupCommand::class
  override fun handle(command: GroupCommand): GroupCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid),
        data = GroupEvent(),
      )
    }
    services.eventBus.handle(event).run(services.state::persist)
    services.event.save(event)

    services.event.setParentEvent(event)
    val commandsResponse = services.commandBus.dispatch(command.commands)
    services.event.clearParentEvent()

    return GroupCommandResult(
      commands = commandsResponse.commands,
    )
  }
}
