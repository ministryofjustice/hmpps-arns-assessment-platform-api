package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

class CreateCollectionCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<CreateCollectionCommand> {
  override val type = CreateCollectionCommand::class
  override fun handle(command: CreateCollectionCommand): CreateCollectionCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.eventBus.persistenceContext.findUserDetails(user),
        assessment = services.eventBus.persistenceContext.findAssessment(assessmentUuid.value),
        data = CollectionCreatedEvent(collectionUuid, name, parentCollectionItemUuid?.value),
        createdAt = services.clock.requestDateTime(),
      )
    }

    services.eventBus.handle(event).createTimeline(command.timeline)

    return CreateCollectionCommandResult(command.collectionUuid)
  }
}
