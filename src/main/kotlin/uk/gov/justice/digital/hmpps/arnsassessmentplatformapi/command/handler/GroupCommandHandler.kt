package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

class GroupCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<GroupCommand> {
  override val type = GroupCommand::class
  override fun handle(command: GroupCommand): GroupCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid.value),
        data = GroupEvent(command.commands.count()),
        createdAt = services.clock.requestDateTime(),
      )
    }
    services.eventBus.handle(event)
    services.event.save(event).run(services.event::setParentEvent)

    val commandsResponse = services.commandBus.dispatch(command.commands)
    services.event.clearParentEvent()

    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(),
      ),
    )

    return GroupCommandResult(
      commands = commandsResponse.commands,
    )
  }
}
