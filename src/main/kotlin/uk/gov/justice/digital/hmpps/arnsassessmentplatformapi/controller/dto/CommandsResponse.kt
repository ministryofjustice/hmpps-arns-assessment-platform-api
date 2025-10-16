package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.result.CommandResult

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)

data class CommandsResponse(
  val commands: List<CommandResponse>,
)
