package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.util.UUID

@Service
class AssessmentService(
  private val commandExecutorHelper: CommandExecutorHelper,
) : CommandExecutor {
  override fun executeCommands(request: CommandRequest) {
    val events = request.commands.mapNotNull { command -> createEvent(command, request.user, request.assessmentUuid) }
    commandExecutorHelper.handleSave(events)
  }

  private fun createEvent(command: Command, user: User, assessmentUuid: UUID): EventEntity? {
    val assessment = when (command) {
      is CreateAssessment -> commandExecutorHelper.createAssessment()
      else -> commandExecutorHelper.fetchAssessment(assessmentUuid)
    }

    return when (command) {
      is CreateAssessment,
      is UpdateAnswers,
      is UpdateFormVersion,
      -> command.toEvent()

      else -> null
    }?.let { domainEvent -> EventEntity.from(assessment, user, domainEvent) }
  }
}
