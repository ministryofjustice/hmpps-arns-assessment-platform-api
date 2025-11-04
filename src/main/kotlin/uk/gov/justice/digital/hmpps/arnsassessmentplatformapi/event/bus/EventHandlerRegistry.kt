package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlerNotImplementedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler.EventHandler
import kotlin.reflect.KClass

@Component
class EventHandlerRegistry(
  handlers: List<EventHandler<out Event>>,
) {
  private val registry: Map<KClass<out Event>, EventHandler<out Event>> = handlers.associateBy { it.type }

  fun getHandlerFor(event: KClass<out Event>) = registry[event] ?: throw EventHandlerNotImplementedException("No handler registered for event: ${event.simpleName}")
}
