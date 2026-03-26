package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus

data class CommandHandlerServiceBundle(
  val eventBus: EventBus,
  val clock: Clock,
)
