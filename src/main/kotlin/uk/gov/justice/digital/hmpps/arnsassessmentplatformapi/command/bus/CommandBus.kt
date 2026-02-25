package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.TestableCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import java.util.UUID

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
      acc + CommandResponse(command, handle(command))
    },
  )
}

data class PlaceholderNotFoundException(val index: Int) : AssessmentPlatformException(
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

fun Reference.resolvePlaceholder(context: List<CommandResponse>) = when (this) {
  is Reference.Uuid -> this
  is Reference.Placeholder -> Reference.Uuid(context.getUuidAt(index))
}

fun Command.resolvePlaceholders(context: List<CommandResponse>): Command {
  return when (this) {
    is AddCollectionItemCommand -> apply { collectionUuid = collectionUuid.resolvePlaceholder(context) }
    is CreateAssessmentCommand -> TODO()
    is CreateCollectionCommand -> TODO()
    is GroupCommand -> TODO()
    is RemoveCollectionItemCommand -> TODO()
    is ReorderCollectionItemCommand -> TODO()
    is RollbackCommand -> TODO()
    is UpdateAssessmentAnswersCommand -> TODO()
    is UpdateAssessmentPropertiesCommand -> TODO()
    is UpdateCollectionItemAnswersCommand -> TODO()
    is UpdateCollectionItemPropertiesCommand -> TODO()
    is UpdateFormVersionCommand -> TODO()
    is TestableCommand -> TODO()
  }
}
