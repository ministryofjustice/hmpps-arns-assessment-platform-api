package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CollectionService
import java.time.Clock
import java.time.LocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Component
class RollbackAnswersCommandHandler(
  private val collectionService: CollectionService,
  private val aggregateService: AggregateService,
  private val eventBus: EventBus,
  private val clock: Clock,
) : CommandHandler<RollbackAnswersCommand> {
  private fun now() = LocalDateTime.now(clock)
  override val type = RollbackAnswersCommand::class
  override fun handle(command: RollbackAnswersCommand): CommandSuccessCommandResult {
    val assessment = collectionService.findByUuid(command.collectionUuid)
    val aggregateType = AssessmentVersionAggregate::class
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
        collection = assessment,
        data = AnswersRolledBackEvent(
          rolledBackTo = command.pointInTime,
          added = answersAdded,
          removed = answersRemoved,
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
