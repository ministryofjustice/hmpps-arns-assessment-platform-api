package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class AddCollectionItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<AddCollectionItemCommand> {
  override val type = AddCollectionItemCommand::class
  override fun handle(command: AddCollectionItemCommand): AddCollectionItemCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid),
        data = CollectionItemAddedEvent(
          collectionItemUuid,
          collectionUuid,
          answers,
          properties,
          index,
        ),
      )
    }

    val collection = services.eventBus.handle(event)
      .also { updatedState -> services.state.persist(updatedState) }
      .run { get(AssessmentAggregate::class) as AssessmentState }
      .getForRead().data.getCollection(command.collectionUuid)
      ?: throw CollectionNotFoundException(command.collectionUuid)

    services.event.save(event)
    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "index" to (command.index ?: (collection.items.size - 1)),
          "collection" to collection.name,
          "collectionItemUuid" to event.data.collectionItemUuid,
          "collectionUuid" to event.data.collectionUuid,
        ),
      ),
    )

    return AddCollectionItemCommandResult(command.collectionItemUuid)
  }
}
