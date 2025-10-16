package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("CommandSuccessResult")
data class CommandSuccessResult(
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}
