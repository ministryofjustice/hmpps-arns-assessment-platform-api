package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext

@Component
class EventBusFactory(
  private val registry: EventHandlerRegistry,
) {
  fun create(persistenceContext: PersistenceContext) = EventBus(
    registry,
    persistenceContext,
  )
}
