package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
data class CommandHandlerServiceBundleFactory(
    val assessment: AssessmentService,
    val event: EventService,
    val state: StateService,
    val userDetails: UserDetailsService,
    val timeline: TimelineService,
    val clock: Clock,
) {
  fun create(eventBus: EventBus, commandBus: CommandBus) = CommandHandlerServiceBundle(
    assessment = assessment,
    event = event,
    state = state,
    userDetails = userDetails,
    timeline = timeline,
    eventBus = eventBus,
    commandBus = commandBus,
    clock = clock,
  )
}
