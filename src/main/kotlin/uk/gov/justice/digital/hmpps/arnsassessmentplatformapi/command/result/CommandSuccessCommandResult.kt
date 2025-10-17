package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result

data class CommandSuccessCommandResult(
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}
