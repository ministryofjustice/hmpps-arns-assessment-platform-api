package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RollbackAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateFormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.Event
import java.time.LocalDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val commandExecutorHelper: CommandExecutorHelper,
  private val aggregateService: AggregateService,
) : CommandExecutor {
  override fun execute(request: CommandRequest): List<EventEntity> = request.commands.mapNotNull { command -> createEvent(command, request.user, request.assessmentUuid) }

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

      is RollbackAssessment -> command.toEvent(assessment)

      else -> null
    }?.let { domainEvent -> EventEntity.from(assessment, user, domainEvent) }
  }

  fun RollbackAssessment.toEvent(assessment: AssessmentEntity): Event {
    val aggregateType = AssessmentVersionAggregate.aggregateType
    val currentVersion: AggregateEntity = aggregateService.fetchAggregateForTypeOnDate(
      assessment,
      aggregateType,
      LocalDateTime.now(),
    ) ?: aggregateService.createAggregateForPointInTime(assessment, aggregateType, LocalDateTime.now())
    val previousVersion: AggregateEntity = aggregateService.fetchAggregateForTypeOnDate(
      assessment,
      aggregateType,
      dateAndTime,
    ) ?: aggregateService.createAggregateForPointInTime(assessment, aggregateType, dateAndTime)

    val currentAnswers = currentVersion.run { data as AssessmentVersionAggregate }.getAnswers()
    val previousAnswers = previousVersion.run { data as AssessmentVersionAggregate }.getAnswers()

    return AnswersRolledBack(
      added = buildMap {
        for ((key, oldValue) in previousAnswers) {
          if (currentAnswers[key] != oldValue) {
            put(key, oldValue)
          }
        }
      },
      removed = currentAnswers.keys.filter { !previousAnswers.contains(it) },
    )
  }
}
