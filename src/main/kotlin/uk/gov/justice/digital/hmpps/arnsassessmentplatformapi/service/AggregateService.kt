package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.time.Clock
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Service
class AggregateService(
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  private val eventBus: EventBus,
  private val clock: Clock,
) {
  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  inner class State<A: Aggregate<A>>(
    private val type: KClass<A>,
  ) {
    fun createState(aggregateEntity: AggregateEntity<A>): AggregateState<A> =
      when (type) {
        AssessmentAggregate::class -> AssessmentState(aggregateEntity as AggregateEntity<AssessmentAggregate>) as AggregateState<A>
        else -> throw Error("Unexpected aggregate type : $type")
      }

    fun blankState(assessment: AssessmentEntity): AggregateState<A> = AggregateEntity(
      assessment = assessment,
      data = type.createInstance(),
      eventsFrom = assessment.createdAt,
      eventsTo = assessment.createdAt,
      updatedAt = now(),
    ).run(::createState)

    fun fetchState(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime?,
    ): AggregateState<A> = when (pointInTime) {
      null -> fetchLatestState(assessment)
      else -> fetchStateForExactPointInTime(assessment, pointInTime)
    }

    fun fetchLatestState(assessment: AssessmentEntity): AggregateState<A> =
      aggregateRepository.findByAssessmentBeforeDate(assessment.uuid, now())
        ?.let { it as AggregateEntity<A> }
        ?.run(::createState)
        ?: createStateForPointInTime(assessment, now())

    fun fetchStateForExactPointInTime(assessment: AssessmentEntity, pointInTime: LocalDateTime): AggregateState<A> =
      aggregateRepository.findByAssessmentOnExactDate(assessment.uuid, pointInTime)
        ?.let { it as AggregateEntity<A> }
        ?.run(::createState)
        ?: createStateForPointInTime(assessment, pointInTime)

    fun createStateForPointInTime(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime,
    ): AggregateState<A> {
      val events = eventService
        .findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
        .sortedBy { it.createdAt }

      val base = events.maxByOrNull { it.createdAt }?.let { latestEvent ->
        aggregateRepository.findByAssessmentBeforeDate(assessment.uuid, pointInTime)
          ?.let { it as AggregateEntity<A> }
          ?.run(::createState)
      } ?: blankState(assessment)

      return events.fold(base) { acc, event -> eventBus.handle(event) }
    }

    fun persist(state: AggregateState<A>) {
      aggregateRepository.saveAll(state.aggregates)
    }
  }
}
