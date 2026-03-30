package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class UpdateCollectionItemPropertiesCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UpdateCollectionItemPropertiesCommand> {
  override val type = UpdateCollectionItemPropertiesCommand::class
  override fun handle(command: UpdateCollectionItemPropertiesCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.persistenceContext.findUserDetails(user),
        assessment = services.persistenceContext.findAssessment(assessmentUuid.value),
        data = CollectionItemPropertiesUpdatedEvent(
          collectionItemUuid = collectionItemUuid.value,
          added = added,
          removed = removed,
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return CommandSuccessCommandResult()
  }
}
