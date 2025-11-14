package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CollectionItemAddedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemAddedEvent> {
  override val eventType = CollectionItemAddedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemAddedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val collectionItem = with(event.data) {
      CollectionItem(
        uuid = collectionItemUuid,
        answers = answers.toMutableMap(),
        properties = properties.toMutableMap(),
        collections = mutableListOf(),
        createdAt = event.createdAt,
        updatedAt = event.createdAt,
      )
    }

    val aggregate = state.get()
    val collection = aggregate.data.getCollection(event.data.collectionUuid)

    event.data.index?.let { index ->
      collection.items.add(index, collectionItem)
    } ?: collection.items.add(collectionItem)

    aggregate.data.apply {
      collaborators.add(event.user)
      event.data.timeline?.item(event)?.run(timeline::add)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
