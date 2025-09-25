package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
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
  override fun execute(request: CommandExecutorRequest): CommandExecutorResult = request.commands.fold(CommandExecutorResult(assessmentUuid = request.assessmentUuid)) { result, command ->
    val event = createEvent(command, request.user, result.getAssessmentUuid() ?: request.assessmentUuid)
    CommandExecutorResult(
      events = event?.let { result.events.plus(event) } ?: result.events,
      assessmentUuid = event?.assessment?.uuid,
    )
  }

  private fun createEvent(command: Command, user: User, assessmentUuid: UUID?): EventEntity? {
    val assessment = when (command) {
      is CreateAssessment -> commandExecutorHelper.createAssessment()
      else -> {
        if (assessmentUuid == null) throw Exception("Missing assessment UUID")
        commandExecutorHelper.fetchAssessment(assessmentUuid)
      }
    }

    return when (command) {
      is CreateAssessment -> command.toEvent()
      is UpdateAnswers -> command.toEvent()
      is UpdateFormVersion -> command.toEvent()
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
      pointInTime,
    ) ?: aggregateService.createAggregateForPointInTime(assessment, aggregateType, pointInTime)

    val currentAnswers = currentVersion.run { data as AssessmentVersionAggregate }.getAnswers()
    val previousAnswers = previousVersion.run { data as AssessmentVersionAggregate }.getAnswers()

    return AnswersRolledBack(
      rolledBackTo = pointInTime,
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
