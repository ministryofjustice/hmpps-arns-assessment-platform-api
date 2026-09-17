package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UndeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

class UndeleteCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UndeleteCommand> {
  override val type = UndeleteCommand::class
  override fun handle(command: UndeleteCommand): CommandSuccessCommandResult {
    with(services.persistenceContext) {
      val assessment = findAssessment(command.assessmentUuid.value)

      eventService.undelete(assessment.uuid, command.pointInTime)
      timelineService.undelete(assessment.uuid, command.pointInTime)
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
            customType = it.type,
            customData = it.data,
          ),
        )
      }
    }

    return CommandSuccessCommandResult()
  }
}
