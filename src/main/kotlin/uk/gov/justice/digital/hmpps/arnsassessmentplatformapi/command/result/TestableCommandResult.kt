package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result

data class TestableCommandResult(
  override val message: String,
  override val success: Boolean = true,
) : CommandResult
