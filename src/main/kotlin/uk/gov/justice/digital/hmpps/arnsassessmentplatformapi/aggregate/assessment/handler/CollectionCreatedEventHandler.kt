package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CollectionCreatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionCreatedEvent> {
  override val eventType = CollectionCreatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionCreatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val collection = with(event.data) {
      Collection(
        uuid = collectionUuid,
        name = name,
        items = mutableListOf(),
        createdAt = event.createdAt,
        updatedAt = event.createdAt,
      )
    }

    val aggregate = state.getForWrite()

    val collections = if (event.data.parentCollectionItemUuid != null) {
      aggregate.data.getCollectionItem(event.data.parentCollectionItemUuid)?.collections
        ?: throw CollectionItemNotFoundException(event.data.parentCollectionItemUuid, aggregate.uuid)
    } else {
      aggregate.data.collections
    }

    collections.add(collection)
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
