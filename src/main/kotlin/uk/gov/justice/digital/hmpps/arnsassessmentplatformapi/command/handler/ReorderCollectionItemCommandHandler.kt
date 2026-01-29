package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class ReorderCollectionItemCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<ReorderCollectionItemCommand> {
  override val type = ReorderCollectionItemCommand::class
  override fun handle(command: ReorderCollectionItemCommand): CommandSuccessCommandResult {
    val assessment = services.assessment.findBy(command.assessmentUuid)
    val state = services.state
      .stateForType(AssessmentAggregate::class)
      .fetchOrCreateLatestState(assessment) as AssessmentState
    val collection = state.getForRead().data.getCollectionWithItem(command.collectionItemUuid)
      ?: throw Error("Collection with item ${command.collectionItemUuid} not found")

    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid),
        data = CollectionItemReorderedEvent(
          collectionItemUuid = collectionItemUuid,
          index = command.index,
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
          "collectionItemUuid" to command.collectionItemUuid,
          "index" to command.index,
          "previousIndex" to collection.items.indexOf(collection.findItem(command.collectionItemUuid)),
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
