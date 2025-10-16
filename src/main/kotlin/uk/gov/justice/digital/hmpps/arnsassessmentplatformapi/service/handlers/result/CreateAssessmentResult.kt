package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.result

import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

@JsonTypeName("CreateAssessmentResult")
data class CreateAssessmentResult(
  val assessmentUuid: UUID,
) : CommandResult {
  override val message = "Assessment created successfully with UUID $assessmentUuid"
  override val success = true
}
