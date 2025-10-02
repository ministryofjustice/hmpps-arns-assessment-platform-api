package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import java.util.UUID

class CreateAssessmentResponse(
  val assessmentUuid: UUID,
) {
  companion object {
    fun from(command: CreateAssessment) = CreateAssessmentResponse(command.assessmentUuid)
  }
}
