package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
class EventBusFactory(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
  val timelineService: TimelineService,
  val userDetailsService: UserDetailsService,
  val assessmentService: AssessmentService,
) {
  fun create() = EventBus(
    registry,
    stateService,
    eventService,
    PersistenceContext(
      stateService,
      eventService,
      timelineService,
      userDetailsService,
      assessmentService,
    ),
  )
}
