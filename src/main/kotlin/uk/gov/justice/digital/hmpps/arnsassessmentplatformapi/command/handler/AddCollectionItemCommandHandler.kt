package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class AddCollectionItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<AddCollectionItemCommand> {
  override val type = AddCollectionItemCommand::class
  override fun handle(command: AddCollectionItemCommand): AddCollectionItemCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid.value),
        data = CollectionItemAddedEvent(
          collectionItemUuid,
          collectionUuid.value,
          answers,
          properties,
          index,
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return AddCollectionItemCommandResult(command.collectionItemUuid)
  }
}
