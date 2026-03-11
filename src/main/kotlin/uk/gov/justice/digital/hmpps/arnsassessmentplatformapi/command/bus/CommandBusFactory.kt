package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundleFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBusFactory

@Component
class CommandBusFactory(
  private val eventBusFactory: EventBusFactory,
  private val commandHandlerFactory: CommandHandlerFactory,
  private val serviceBundleFactory: CommandHandlerServiceBundleFactory,
) {
  fun create() = CommandBus(
    eventBusFactory.create(),
    commandHandlerFactory,
    serviceBundleFactory,
  )
}
