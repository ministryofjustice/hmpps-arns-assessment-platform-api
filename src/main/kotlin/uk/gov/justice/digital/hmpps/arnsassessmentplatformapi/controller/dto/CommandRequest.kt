package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.AddOasysEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RollbackAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException
import java.util.UUID

data class CommandRequest(
  val user: User,
  val commands: List<Command>,
  val assessmentUuid: UUID? = null,
) {
  init {
    val unsupportedCommands =
      commands.map { it::class }.filter { !supportedCommands.contains(it) }.map { it.simpleName }.distinct()

    if (commands.isEmpty()) throw InvalidCommandException("No commands received")
    if (unsupportedCommands.isNotEmpty()) throw InvalidCommandException("Request contains unsupported commands $unsupportedCommands, supported commands ${supportedCommands.map { it.simpleName }}")
  }

  companion object {
    val supportedCommands =
      listOf(UpdateAnswers::class, RollbackAssessment::class, UpdateFormVersion::class, AddOasysEvent::class)
  }
}
