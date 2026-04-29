package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.SoftDeleteEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

class SoftDeleteCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<SoftDeleteCommand> {
  override val type = SoftDeleteCommand::class
  override fun handle(command: SoftDeleteCommand): CommandSuccessCommandResult {
    with(services.persistenceContext) {
      val assessment = findAssessment(command.assessmentUuid.value)

      eventService.softDelete(assessment.uuid, command.pointInTime)
      timelineService.softDelete(assessment.uuid, command.pointInTime)
      stateService.delete(assessment.uuid)

      stateService.rebuildFromEvents(assessment, null).let {
        stateService.persist(mutableMapOf(assessment.uuid to it))
      }

      command.timeline?.let {
        timeline.add(
          TimelineEntity(
            createdAt = services.clock.requestDateTime(),
            user = findUserDetails(command.user),
            assessment = assessment,
            eventType = SoftDeleteEvent::class.simpleName,
            customType = it.type,
            customData = it.data,
          ),
        )
      }
    }

    return CommandSuccessCommandResult()
  }
}
