package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService

data class CommandHandlerServiceBundle(
  val timeline: TimelineService,
  val eventBus: EventBus,
  val clock: Clock,
)
