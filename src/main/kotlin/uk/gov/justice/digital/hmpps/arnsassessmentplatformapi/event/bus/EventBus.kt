package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.reflect.full.createInstance

@Component
class EventBus(
  val registry: EventHandlerRegistry,
) {
  private fun <E : Event, S : State> execute(event: EventEntity<E>, state: S): S {
    registry.getHandlersFor(event.data::class).map { handler ->
      val aggregateType = handler.stateType.createInstance().type
      state[aggregateType] = handler.execute(event, state[aggregateType])
    }
    return state
  }

  fun handle(event: EventEntity<*>) = handle(listOf(event))

  fun handle(events: List<EventEntity<*>>) = events.fold(mutableMapOf()) { acc: State, event -> execute(event, acc) }
}
