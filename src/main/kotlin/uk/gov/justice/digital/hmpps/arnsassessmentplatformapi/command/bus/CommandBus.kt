package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Service
class CommandBus(
  private val registry: CommandHandlerRegistry,
) {
  private fun handle(command: Command) = registry.getHandlerFor(command::class).execute(command)

  @Transactional
  fun dispatch(command: Command) = dispatch(listOf(command))

  @Transactional
  fun dispatch(commands: List<Command>) = CommandsResponse(
    commands.fold(emptyList()) { acc, command ->
      command.resolvePlaceholders(acc)
      acc + CommandResponse(command, handle(command))
    },
  )
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
