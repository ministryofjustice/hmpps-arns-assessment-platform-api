package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class GroupCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
  @param:Lazy private val commandBus: CommandBus,
) : CommandHandler<GroupCommand> {
  override val type = GroupCommand::class
  override fun handle(command: GroupCommand): GroupCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findBy(UuidIdentifier(assessmentUuid)),
        data = GroupEvent(timeline),
      )
    }
    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    eventService.setParentEvent(event)
    val commandsResponse = commandBus.dispatch(command.commands)
    eventService.clearParentEvent()

    return GroupCommandResult(
      commands = commandsResponse.commands,
    )
  }
}
