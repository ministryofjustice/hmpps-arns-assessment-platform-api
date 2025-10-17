package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result

import java.util.UUID

data class CreateAssessmentCommandResult(
  val assessmentUuid: UUID,
) : CommandResult {
  override val message = "Assessment created successfully with UUID $assessmentUuid"
  override val success = true
}
