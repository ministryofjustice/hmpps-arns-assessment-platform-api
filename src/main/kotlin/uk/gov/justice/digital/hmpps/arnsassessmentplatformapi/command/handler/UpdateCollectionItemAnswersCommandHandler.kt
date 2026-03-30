package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class UpdateCollectionItemAnswersCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UpdateCollectionItemAnswersCommand> {
  override val type = UpdateCollectionItemAnswersCommand::class
  override fun handle(command: UpdateCollectionItemAnswersCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.persistenceContext.findUserDetails(user),
        assessment = services.persistenceContext.findAssessment(assessmentUuid.value),
        data = CollectionItemAnswersUpdatedEvent(
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
