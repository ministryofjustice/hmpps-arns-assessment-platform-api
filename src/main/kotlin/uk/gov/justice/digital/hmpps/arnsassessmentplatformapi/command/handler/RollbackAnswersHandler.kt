package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import java.time.Clock
import java.time.LocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Component
class RollbackAnswersHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
  private val eventBus: EventBus,
  private val clock: Clock,
) : CommandHandler<RollbackAnswers> {
  private fun now() = LocalDateTime.now(clock)
  override val type = RollbackAnswers::class
  override fun handle(command: RollbackAnswers): CommandSuccessResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    val aggregateType = AssessmentVersionAggregate.aggregateType
    val currentVersion: AggregateEntity = aggregateService.fetchAggregateForExactPointInTime(
      assessment,
      aggregateType,
      now(),
    ) ?: aggregateService.createAggregateForPointInTime(assessment, aggregateType, now())
    val previousVersion: AggregateEntity = aggregateService.fetchAggregateForExactPointInTime(
      assessment,
      aggregateType,
      command.pointInTime,
    ) ?: aggregateService.createAggregateForPointInTime(assessment, aggregateType, command.pointInTime)

    val currentAnswers = currentVersion.run { data as AssessmentVersionAggregate }.getAnswers()
    val previousAnswers = previousVersion.run { data as AssessmentVersionAggregate }.getAnswers()

    val answersAdded = buildMap {
      for ((key, oldValue) in previousAnswers) {
        if (currentAnswers[key] != oldValue) {
          put(key, oldValue)
        }
      }
    }
    val answersRemoved = currentAnswers.keys.filter { !previousAnswers.contains(it) }

    eventBus.add(
      EventEntity(
        user = command.user,
        assessment = assessment,
        data = AnswersRolledBack(
          rolledBackTo = command.pointInTime,
          added = answersAdded,
          removed = answersRemoved,
        ),
      ),
    )

    return CommandSuccessResult()
  }
}
