package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.Clock
import java.time.LocalDateTime

@Component
class CollectionItemReorderedEventHandler(
  private val clock: Clock,
  stateService: StateService,
) : AssessmentEventHandler<CollectionItemReorderedEvent>(stateService) {
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

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
