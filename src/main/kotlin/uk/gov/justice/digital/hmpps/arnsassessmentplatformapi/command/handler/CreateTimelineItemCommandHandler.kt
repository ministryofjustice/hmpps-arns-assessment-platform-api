package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

class CreateTimelineItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<CreateTimelineItemCommand> {
  override val type = CreateTimelineItemCommand::class
  override fun handle(command: CreateTimelineItemCommand): CommandSuccessCommandResult {
    val assessment = services.eventBus.persistenceContext.findAssessment(command.assessmentUuid.value)
    val user = services.eventBus.persistenceContext.findUserDetails(command.user)

    services.eventBus.persistenceContext.timeline.add(
      TimelineEntity(
        createdAt = command.timestamp,
        user = user,
        assessment = assessment,
        customType = command.timeline.type,
        customData = command.timeline.data,
      ),
    )

    return CommandSuccessCommandResult()
  }
}
