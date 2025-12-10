package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CollectionItemPropertiesUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemPropertiesUpdatedEvent> {
  override val eventType = CollectionItemPropertiesUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemPropertiesUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.getForWrite()

    aggregate.data.getCollectionItem(event.data.collectionItemUuid).run {
      updatedAt = event.createdAt
      event.data.added.forEach { properties.put(it.key, it.value) }
      event.data.removed.forEach { properties.remove(it) }
    }

    aggregate.data.apply {
      collaborators.add(event.user)
      event.data.timeline?.let { timeline.add(it.item(event)) }
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
