package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class CollectionItemRemovedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemRemovedEvent> {
  override val eventType = CollectionItemRemovedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemRemovedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    if (!aggregate.data.collections.any { collection -> collection.removeItem(event.data.collectionItemUuid) }) {
      throw Error("Collection item ID ${event.data.collectionItemUuid} does not exist")
    }

    aggregate.data.collaborators.add(event.user)

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
