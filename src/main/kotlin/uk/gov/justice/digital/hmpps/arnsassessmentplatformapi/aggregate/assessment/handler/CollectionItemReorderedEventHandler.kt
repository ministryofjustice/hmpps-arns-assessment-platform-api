package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class CollectionItemReorderedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemReorderedEvent> {
  override val eventType = CollectionItemReorderedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemReorderedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    if (!aggregate.data.collections.any { collection -> collection.reorderItem(event.data.collectionItemUuid, event.data.index) }) {
      throw Error("Collection item ID ${event.data.collectionItemUuid} does not exist")
    }

    aggregate.data.apply {
      collaborators.add(event.user)
      event.data.timeline?.run(timeline::add)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
