package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RollbackAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAssessmentStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion

class InvalidCommandException(
  message: String,
) : AssessmentPlatformException(
  message = "Unable to process commands",
  developerMessage = message,
  statusCode = HttpStatus.BAD_REQUEST,
)

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
      listOf(UpdateAnswers::class, RollbackAnswers::class, UpdateFormVersion::class, UpdateAssessmentStatus::class)
  }
}
