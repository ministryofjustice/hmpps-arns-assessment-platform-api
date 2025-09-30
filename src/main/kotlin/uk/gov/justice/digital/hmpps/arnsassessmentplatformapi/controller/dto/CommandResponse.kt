package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CommandExecutorResult
import java.util.UUID

class CommandResponse(
  val assessmentUuid: UUID?,
) {
  companion object {
    fun from(commandExecutorResult: CommandExecutorResult): CommandResponse = CommandResponse(commandExecutorResult.getAssessmentUuid())
  }
}
