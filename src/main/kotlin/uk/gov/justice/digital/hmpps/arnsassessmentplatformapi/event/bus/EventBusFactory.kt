package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class EventBusFactory(
  private val registry: EventHandlerRegistry,
  private val stateService: StateService,
) {
  fun create(persistenceContext: PersistenceContext) = EventBus(
    registry,
    stateService,
    persistenceContext,
  )
}
