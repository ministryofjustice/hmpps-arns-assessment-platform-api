package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBusFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContextFactory

@Component
class CommandBusFactory(
  private val eventBusFactory: EventBusFactory,
  private val commandHandlerFactory: CommandHandlerFactory,
  private val persistenceContextFactory: PersistenceContextFactory,
  private val clock: Clock,
) {
  fun create(): CommandBus {
    val persistenceContext = persistenceContextFactory.create()
    return CommandBus(
      commandHandlerFactory,
      CommandHandlerServiceBundle(
        persistenceContext = persistenceContext,
        eventBus = eventBusFactory.create(persistenceContext),
        clock = clock,
      ),
    )
  }
}
