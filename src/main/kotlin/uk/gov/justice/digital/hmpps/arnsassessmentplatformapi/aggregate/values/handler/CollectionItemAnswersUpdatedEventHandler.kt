package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Component
class CollectionItemAnswersUpdatedEventHandler(
  private val clock: Clock,
) : ValuesEventHandler<CollectionItemAnswersUpdatedEvent> {
  override val eventType = CollectionItemAnswersUpdatedEvent::class
  override val stateType = ValuesState::class

  override fun handle(
    event: EventEntity<CollectionItemAnswersUpdatedEvent>,
    state: ValuesState,
  ): ValuesState {
    state.getForWrite().apply {
      for ((key, value) in event.data.added) {
        data.addAnswer(ValueId.of(key, event.data.collectionItemUuid), value)
      }
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
