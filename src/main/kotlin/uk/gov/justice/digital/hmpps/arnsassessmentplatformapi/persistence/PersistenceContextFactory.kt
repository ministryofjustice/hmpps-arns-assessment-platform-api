package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
class PersistenceContextFactory(
  private val stateService: StateService,
  private val eventService: EventService,
  private val timelineService: TimelineService,
  private val userDetailsService: UserDetailsService,
  private val assessmentService: AssessmentService,
) {
  fun create() = PersistenceContext(
    stateService,
    eventService,
    timelineService,
    userDetailsService,
    assessmentService,
  )
}
