package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class CollectionItemPropertiesUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemPropertiesUpdatedEvent> {
  override val eventType = CollectionItemPropertiesUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemPropertiesUpdatedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    val aggregate = state.getForWrite(clock)

    val collection = aggregate.data.getCollectionWithItem(event.data.collectionItemUuid)
    val item = collection?.findItem(event.data.collectionItemUuid)

    if (collection == null || item == null) {
      throw CollectionItemNotFoundException(event.data.collectionItemUuid, aggregate.uuid)
    }

    item.run {
      updatedAt = event.createdAt
      event.data.added.forEach { properties.put(it.key, it.value) }
      event.data.removed.forEach { properties.remove(it) }
    }

    aggregate.data.apply {
      collaborators.add(event.user.uuid)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return EventHandlerResult(
      state = state,
      timeline = TimelineEntity.resolver(
        event,
        mapOf(
          "collection" to collection.name,
          "index" to collection.items.indexOf(item),
          "added" to event.data.added.keys,
          "removed" to event.data.removed,
        ),
      ),
    )
  }
}
