package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class UpdateCollectionItemAnswersCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<UpdateCollectionItemAnswersCommand> {
  override val type = UpdateCollectionItemAnswersCommand::class
  override fun handle(command: UpdateCollectionItemAnswersCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid),
        data = CollectionItemAnswersUpdatedEvent(
          collectionItemUuid = collectionItemUuid,
          added = added,
          removed = removed,
        ),
      )
    }

    val collection = services.eventBus.handle(event)
      .also { updatedState -> services.state.persist(updatedState) }
      .run { get(AssessmentAggregate::class) as AssessmentState }
      .getForRead().data.getCollection(command.collectionItemUuid)
      ?: throw Error("Collection $command.collectionItemUuid not found")

    services.event.save(event)
    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "collection" to collection.name,
          "index" to collection.findItem(command.collectionItemUuid).run(collection.items::indexOf),
          "added" to command.added.keys,
          "removed" to command.removed,
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
