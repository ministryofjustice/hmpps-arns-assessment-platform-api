package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService

@Component
data class CommandHandlerServiceBundleFactory(
  val timeline: TimelineService,
  val clock: Clock,
) {
  fun create(eventBus: EventBus) = CommandHandlerServiceBundle(
    timeline = timeline,
    eventBus = eventBus,
    clock = clock,
  )
}
