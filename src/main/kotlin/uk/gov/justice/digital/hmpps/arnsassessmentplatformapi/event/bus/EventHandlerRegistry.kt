package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlerNotImplementedException
import kotlin.reflect.KClass

@Component
class EventHandlerRegistry(
  handlers: List<EventHandler<out Event, out AggregateState<out Aggregate<*>>>>,
) {
  private val registry = handlers.groupBy { it.eventType }

  fun <E : Event> getHandlersFor(eventType: KClass<out E>): List<EventHandler<E, AggregateState<out Aggregate<*>>>> = @Suppress("UNCHECKED_CAST")
  registry[eventType]?.mapNotNull { it as? EventHandler<E, AggregateState<out Aggregate<*>>> }
    ?: throw EventHandlerNotImplementedException("No handlers registered for event ${eventType.simpleName}")
}
