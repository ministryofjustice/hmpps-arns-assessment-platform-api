package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.AddOasysEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RollbackAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException

data class CommandRequest(
  val commands: List<Command>,
) {
  init {
    val unsupportedCommands =
      commands.map { it::class }.filter { !supportedCommands.contains(it) }.map { it.simpleName }.distinct()

    if (commands.isEmpty()) throw InvalidCommandException("No commands received")
    if (unsupportedCommands.isNotEmpty()) throw InvalidCommandException("Request contains unsupported commands $unsupportedCommands, supported commands ${supportedCommands.map { it.simpleName }}")
  }

  companion object {
    val supportedCommands =
      listOf(UpdateAnswers::class, RollbackAnswers::class, UpdateFormVersion::class, AddOasysEvent::class)
  }
}
