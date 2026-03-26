package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus

@Component
data class CommandHandlerServiceBundleFactory(
  val clock: Clock,
) {
  fun create(eventBus: EventBus) = CommandHandlerServiceBundle(
    eventBus = eventBus,
    clock = clock,
  )
}
