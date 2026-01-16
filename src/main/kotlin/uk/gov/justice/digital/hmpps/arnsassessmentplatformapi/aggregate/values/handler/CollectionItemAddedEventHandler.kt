package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Component
class CollectionItemAddedEventHandler(
  private val clock: Clock,
) : ValuesEventHandler<CollectionItemAddedEvent> {
  override val eventType = CollectionItemAddedEvent::class
  override val stateType = ValuesState::class

  override fun handle(
    event: EventEntity<CollectionItemAddedEvent>,
    state: ValuesState,
  ): ValuesState {
    state.getForWrite().apply {
      for ((key, value) in event.data.answers) {
        data.addAnswer(ValueId.of(key, event.data.collectionItemUuid), value)
      }
      for ((key, value) in event.data.properties) {
        data.addProperty(ValueId.of(key, event.data.collectionItemUuid), value)
      }
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
