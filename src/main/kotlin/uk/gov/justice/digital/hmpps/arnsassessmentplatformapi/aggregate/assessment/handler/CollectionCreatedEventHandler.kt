package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.Clock
import java.time.LocalDateTime

@Component
class CollectionCreatedEventHandler(
  private val clock: Clock,
  stateService: StateService,
) : AssessmentEventHandler<CollectionCreatedEvent>(stateService) {
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
      )
    }

    val aggregate = state.get()

    val collections = event.data.parentCollectionItemUuid?.let {
      aggregate.data.getCollectionItem(it).collections
    } ?: aggregate.data.collections

    collections.add(collection)

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
