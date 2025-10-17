package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)
