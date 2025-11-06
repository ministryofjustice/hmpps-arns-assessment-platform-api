package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.time.Clock
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Service
class StateService(
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  private val eventBus: EventBus,
  private val clock: Clock,
) {
  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  fun persist(state: State) {
    aggregateRepository.saveAll(state.values.map { it.aggregates }.flatten())
  }

  inner class ForType<A: Aggregate<A>>(
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
    ): AggregateState<A> =
      eventService
        .findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
        .sortedBy { it.createdAt }
        .run(eventBus::handle)
        .getOrDefault(type, blankState(assessment))
        .let { it as AggregateState<A> }
  }
}
