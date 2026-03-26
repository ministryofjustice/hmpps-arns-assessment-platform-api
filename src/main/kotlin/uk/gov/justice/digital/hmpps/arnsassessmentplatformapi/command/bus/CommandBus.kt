package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundleFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class CommandBus(
  private val eventBus: EventBus,
  private val commandHandlerFactory: CommandHandlerFactory,
  private val serviceBundleFactory: CommandHandlerServiceBundleFactory,
) {
  private fun handle(command: Command): CommandResult {
    val serviceBundle = serviceBundleFactory.create(eventBus)
    val handler = commandHandlerFactory.create(command, serviceBundle)
    return handler.execute(command)
  }

  fun dispatch(commands: List<Command>) = CommandsResponse(
    commands.fold(emptyList()) { acc, command ->
      command.resolvePlaceholders(acc)
      acc + CommandResponse(command, handle(command))
    },
  )

//  @Transactional
  open fun dispatchAndPersist(commands: List<Command>) = dispatch(commands).also { eventBus.persistState() }
}

data class PlaceholderNotFoundException(val index: Int) :
  AssessmentPlatformException(
    "Placeholder value not found @$index",
    developerMessage = "Null value found at index: $index",
    statusCode = HttpStatus.BAD_REQUEST,
  )

fun List<CommandResponse>.getUuidAt(index: Int): UUID = get(index).let {
  when (it.result) {
    is AddCollectionItemCommandResult -> it.result.collectionItemUuid
    is CreateAssessmentCommandResult -> it.result.assessmentUuid
    is CreateCollectionCommandResult -> it.result.collectionUuid
    else -> null
  }
} ?: throw PlaceholderNotFoundException(index)

fun Command.resolvePlaceholders(context: List<CommandResponse>) {
  this::class.memberProperties
    .filterIsInstance<KProperty1<Command, *>>()
    .filter { it.returnType.classifier == Reference::class }
    .forEach { prop ->
      prop.isAccessible = true
      val value = prop.get(this) as Reference?
      value?.resolve(context)
    }
}
