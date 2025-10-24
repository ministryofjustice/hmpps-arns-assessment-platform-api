package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentStatusCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CollectionService

@Component
class UpdateAssessmentStatusCommandHandler(
  private val collectionService: CollectionService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateAssessmentStatusCommand> {
  override val type = UpdateAssessmentStatusCommand::class
  override fun handle(command: UpdateAssessmentStatusCommand): CommandSuccessCommandResult {
    val assessment = collectionService.findByUuid(command.collectionUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          collection = assessment,
          data = AssessmentStatusUpdatedEvent(status),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
