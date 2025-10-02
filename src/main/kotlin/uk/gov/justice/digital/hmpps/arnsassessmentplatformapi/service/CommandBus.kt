package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.CommandHandler
import kotlin.reflect.KClass

@Component
class CommandHandlerRegistry(
  handlers: List<CommandHandler<out Command>>,
) {
  private val registry: Map<KClass<out Command>, CommandHandler<out Command>> = handlers.associateBy { it.type }

  fun getHandlerFor(command: KClass<out Command>) = registry[command] ?: error("No handler for ${command.simpleName}")
}

@Service
class CommandBus(
  private val registry: CommandHandlerRegistry,
) {
  fun dispatch(commands: List<Command>) {
    commands.forEach { command -> registry.getHandlerFor(command::class).execute(command) }
  }
}
