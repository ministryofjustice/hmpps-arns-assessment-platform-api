package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RollbackAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventBus
import java.time.LocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Component
class RollbackAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val aggregateService: AggregateService,
  private val eventBus: EventBus,
) : CommandHandler<RollbackAnswers> {
  override val type = RollbackAnswers::class
  override fun handle(command: RollbackAnswers) {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    val aggregateType = AssessmentVersionAggregate.aggregateType
    val currentVersion: AggregateEntity = aggregateService.fetchAggregateForExactPointInTime(
      assessment,
      aggregateType,
      LocalDateTime.now(),
    ) ?: aggregateService.createAggregateForPointInTime(assessment, aggregateType, LocalDateTime.now())
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
  }
}
