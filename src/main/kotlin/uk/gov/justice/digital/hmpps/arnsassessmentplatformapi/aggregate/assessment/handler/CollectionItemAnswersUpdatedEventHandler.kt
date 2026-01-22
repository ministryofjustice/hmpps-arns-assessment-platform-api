package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CollectionItemAnswersUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemAnswersUpdatedEvent> {
  override val eventType = CollectionItemAnswersUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<CollectionItemAnswersUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.getForWrite()

    aggregate.data.getCollectionItem(event.data.collectionItemUuid)?.run {
      updatedAt = event.createdAt
      event.data.added.forEach { answers.put(it.key, it.value) }
      event.data.removed.forEach { answers.remove(it) }
    } ?: throw CollectionItemNotFoundException(event.data.collectionItemUuid, aggregate.uuid)

    aggregate.data.apply {
      collaborators.add(event.user.uuid)
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
