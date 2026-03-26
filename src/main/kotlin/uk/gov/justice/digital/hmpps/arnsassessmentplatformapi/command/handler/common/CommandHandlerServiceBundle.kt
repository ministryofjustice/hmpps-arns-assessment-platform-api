package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

data class CommandHandlerServiceBundle(
  val assessment: AssessmentService,
  val userDetails: UserDetailsService,
  val timeline: TimelineService,
  val eventBus: EventBus,
  val clock: Clock,
)
