package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse

data class FormVersionUpdatedCommandResult(
  val commands: List<CommandResponse>,
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}
