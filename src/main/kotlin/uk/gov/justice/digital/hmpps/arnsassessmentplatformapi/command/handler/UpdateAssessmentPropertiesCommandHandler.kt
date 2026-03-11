package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

class UpdateAssessmentPropertiesCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UpdateAssessmentPropertiesCommand> {
  override val type = UpdateAssessmentPropertiesCommand::class
  override fun handle(command: UpdateAssessmentPropertiesCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid.value),
        data = AssessmentPropertiesUpdatedEvent(added, removed),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event)
    services.event.save(event)

    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "added" to command.added.keys,
          "removed" to command.removed,
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
