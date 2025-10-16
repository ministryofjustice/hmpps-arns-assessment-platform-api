package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.HandlerNotImplementedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import kotlin.reflect.KClass

@Component
class CommandHandlerRegistry(
  handlers: List<CommandHandler<out Command>>,
) {
  private val registry: Map<KClass<out Command>, CommandHandler<out Command>> = handlers.associateBy { it.type }

  fun getHandlerFor(command: KClass<out Command>) = registry[command] ?: throw HandlerNotImplementedException("No handler registered for type: ${command.simpleName}")
}
