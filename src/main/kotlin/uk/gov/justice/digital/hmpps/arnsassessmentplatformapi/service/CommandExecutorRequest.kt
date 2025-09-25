package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CreateAssessmentRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import java.util.UUID

data class CommandExecutorRequest(
  val user: User,
  val commands: List<Command>,
  val assessmentUuid: UUID? = null,
) {
  companion object {
    fun from(request: CommandRequest) = with(request) { CommandExecutorRequest(user, commands, assessmentUuid) }
    fun from(request: CreateAssessmentRequest) = with(request) { CommandExecutorRequest(user, listOf(CreateAssessment())) }
  }
}
