package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.AddOasysEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.util.UUID

@Service
class OasysService(
  private val commandExecutorHelper: CommandExecutorHelper,
) : CommandExecutor {
  override fun execute(request: CommandRequest): List<EventEntity> = request.commands.mapNotNull { command -> createEvent(command, request.user, request.assessmentUuid) }

  private fun createEvent(command: Command, user: User, assessmentUuid: UUID): EventEntity? {
    val assessment = commandExecutorHelper.fetchAssessment(assessmentUuid)

    return when (command) {
      is AddOasysEvent -> command.toEvent()
      else -> null
    }?.let { domainEvent -> EventEntity.from(assessment, user, domainEvent) }
  }
}
