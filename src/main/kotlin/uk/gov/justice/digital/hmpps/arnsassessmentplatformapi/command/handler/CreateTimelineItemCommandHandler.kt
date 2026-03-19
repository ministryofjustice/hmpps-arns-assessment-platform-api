package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class CreateTimelineItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<CreateTimelineItemCommand> {
  override val type = CreateTimelineItemCommand::class
  override fun handle(command: CreateTimelineItemCommand): CommandSuccessCommandResult {
    val assessment = services.assessment.findBy(command.assessmentUuid.value)
    val user = services.userDetails.findOrCreate(command.user)

    services.timeline.save(
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
