package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
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

    val aggregate = state.getForWrite(clock)
    val collection = aggregate.data.getCollection(event.data.collectionUuid)
      ?: throw CollectionNotFoundException(event.data.collectionUuid, aggregate.uuid)

    if (event.data.index != null) {
      collection.items.add(event.data.index, collectionItem)
    } else {
      collection.items.add(collectionItem)
    }

    aggregate.data.apply {
      collaborators.add(event.user.uuid)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
