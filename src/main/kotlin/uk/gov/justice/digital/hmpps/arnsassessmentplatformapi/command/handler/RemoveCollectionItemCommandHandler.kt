package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class RemoveCollectionItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<RemoveCollectionItemCommand> {
  override val type = RemoveCollectionItemCommand::class
  override fun handle(command: RemoveCollectionItemCommand): CommandSuccessCommandResult {
    val assessment = services.assessment.findBy(command.assessmentUuid)
    val state = services.state
      .stateForType(AssessmentAggregate::class)
      .fetchOrCreateLatestState(assessment) as AssessmentState
    val collection = state.getForRead().data.getCollection(command.collectionItemUuid)
      ?: throw Error("Collection ${command.collectionItemUuid} not found")

    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid),
        data = CollectionItemRemovedEvent(
          collectionItemUuid = collectionItemUuid,
        ),
      )
    }

    services.eventBus.handle(event).run(services.state::persist)
    services.event.save(event)
    services.timeline.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "collection" to collection.name,
          "index" to collection.items.indexOf(collection.findItem(command.collectionItemUuid)),
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
