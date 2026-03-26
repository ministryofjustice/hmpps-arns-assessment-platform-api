package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class RemoveCollectionItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<RemoveCollectionItemCommand> {
  override val type = RemoveCollectionItemCommand::class
  override fun handle(command: RemoveCollectionItemCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.eventBus.persistenceContext.findUserDetails(user),
        assessment = services.eventBus.persistenceContext.findAssessment(assessmentUuid.value),
        data = CollectionItemRemovedEvent(
          collectionItemUuid = collectionItemUuid.value,
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return CommandSuccessCommandResult()
  }
}
