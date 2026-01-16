package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class CollectionItemPropertiesUpdatedEventHandler(
  private val clock: Clock,
) : ValuesEventHandler<CollectionItemPropertiesUpdatedEvent> {
  override val eventType = CollectionItemPropertiesUpdatedEvent::class
  override val stateType = ValuesState::class

  override fun handle(
    event: EventEntity<CollectionItemPropertiesUpdatedEvent>,
    state: ValuesState,
  ): ValuesState {
    state.getForWrite().apply {
      for ((key, value) in event.data.added) {
        data.addProperty(ValueId.of(key, event.data.collectionItemUuid), value)
      }
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
