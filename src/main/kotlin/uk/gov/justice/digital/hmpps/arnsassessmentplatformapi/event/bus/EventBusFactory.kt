package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService

@Component
class EventBusFactory(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
  val timelineService: TimelineService,
) {
  fun create() = EventBus(registry, stateService, eventService, timelineService)
}
