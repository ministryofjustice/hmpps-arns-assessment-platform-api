package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContextFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class EventBusFactory(
  private val registry: EventHandlerRegistry,
  private val stateService: StateService,
  private val eventService: EventService,
  private val persistenceContextFactory: PersistenceContextFactory,
) {
  fun create() = EventBus(
    registry,
    stateService,
    eventService,
    persistenceContextFactory.create(),
  )

  fun create(persistenceContext: PersistenceContext) = EventBus(
    registry,
    stateService,
    eventService,
    persistenceContext,
  )
}
