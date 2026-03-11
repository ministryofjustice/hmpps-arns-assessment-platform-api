package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

class UpdateCollectionItemPropertiesCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UpdateCollectionItemPropertiesCommand> {
  override val type = UpdateCollectionItemPropertiesCommand::class
  override fun handle(command: UpdateCollectionItemPropertiesCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid.value),
        data = CollectionItemPropertiesUpdatedEvent(
          collectionItemUuid = collectionItemUuid.value,
          added = added,
          removed = removed,
        ),
        createdAt = services.clock.requestDateTime(),
      )
    }

    val collection = services.eventBus.handle(event)
      .also { services.event.save(event) }
      .run { get(AssessmentAggregate::class) as AssessmentState }
      .getForRead().data.getCollectionWithItem(command.collectionItemUuid.value)
      ?: throw CollectionItemNotFoundException(command.collectionItemUuid.value)

    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "collection" to collection.name,
          "index" to collection.findItem(command.collectionItemUuid.value).run(collection.items::indexOf),
          "added" to command.added.keys,
          "removed" to command.removed,
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
