package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class ReorderCollectionItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<ReorderCollectionItemCommand> {
  override val type = ReorderCollectionItemCommand::class
  override fun handle(command: ReorderCollectionItemCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.persistenceContext.findUserDetails(user),
        assessment = services.persistenceContext.findAssessment(assessmentUuid.value),
        data = CollectionItemReorderedEvent(
          collectionItemUuid = collectionItemUuid.value,
          index = command.index,
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return CommandSuccessCommandResult()
  }
}
