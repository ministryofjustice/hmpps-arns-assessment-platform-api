package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.Clock
import java.time.LocalDateTime

@Component
class CollectionItemAnswersUpdatedEventHandler(
  private val clock: Clock,
  stateService: StateService,
) : AssessmentEventHandler<CollectionItemAnswersUpdatedEvent>(stateService) {
  override val eventType = CollectionItemAnswersUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemAnswersUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    aggregate.data.getCollectionItem(event.data.collectionItemUuid).run {
      event.data.added.forEach { answers.put(it.key, it.value) }
      event.data.removed.forEach { answers.remove(it) }
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
