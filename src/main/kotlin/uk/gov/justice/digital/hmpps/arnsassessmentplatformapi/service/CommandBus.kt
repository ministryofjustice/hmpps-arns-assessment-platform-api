package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.CommandHandler
import kotlin.reflect.KClass

class HandlerNotImplementedException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Unable to dispatch command",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )

@Component
class CommandHandlerRegistry(
  val handlers: List<CommandHandler<out Command>>,
) {
  private val registry: Map<KClass<out Command>, CommandHandler<out Command>> = handlers.associateBy { it.type }

  fun getHandlerFor(command: KClass<out Command>) = registry[command] ?: throw HandlerNotImplementedException("No handler registered for type: ${command.simpleName}")
}

@Service
class CommandBus(
  private val registry: CommandHandlerRegistry,
  private val eventBus: EventBus,
) {
  fun dispatch(commands: List<Command>) {
    commands.forEach { command -> registry.getHandlerFor(command::class).execute(command) }
    eventBus.commit()
  }
}
