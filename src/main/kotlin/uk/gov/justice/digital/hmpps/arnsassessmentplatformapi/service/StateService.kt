package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AggregateTypeNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.InvalidTimestampException
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Service
class StateService(
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  @param:Lazy val eventBus: EventBus,
  private val clock: Clock,
  private val entityManager: EntityManager,
  private val assessmentVersionCacheService: AssessmentVersionCacheService,
) {
  fun persist(state: State) {
    state.values.flatMap { it.aggregates }
      .run(aggregateRepository::saveAllAndFlush)

    state.values
      .map { aggregateState -> aggregateState.getLatest().assessment.uuid }
      .toSet()
      .forEach(assessmentVersionCacheService::evictLatestAfterCommit)

    state.values.forEach { aggregateState ->
      aggregateState.apply {
        val latest = getLatest()
        val outdated = aggregates.filter { it != latest }.toSet()
        aggregates.removeAll(outdated)
        outdated.forEach { entityManager.detach(it) }
      }
    }
  }

  fun stateForType(type: KClass<out Aggregate<*>>) = StateForType(type)

  inner class StateForType<A : Aggregate<A>>(
    private val type: KClass<A>,
  ) {
    fun createState(aggregateEntity: AggregateEntity<A>): AggregateState<A> = when (type) {
      AssessmentAggregate::class -> AssessmentState(aggregateEntity as AggregateEntity<AssessmentAggregate>) as AggregateState<A>
      else -> throw AggregateTypeNotFoundException(type.simpleName ?: "Unknown")
    }

    fun blankState(assessment: AssessmentEntity): AggregateState<A> = AggregateEntity(
      assessment = assessment,
      data = type.createInstance(),
      eventsFrom = assessment.createdAt,
      eventsTo = assessment.createdAt,
      updatedAt = clock.now(),
    ).run(::createState)

    fun fetchLatestStateBefore(assessment: AssessmentEntity, pointInTime: LocalDateTime): AggregateState<A>? = aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, type.simpleName!!, pointInTime)
      ?.let { it as AggregateEntity<A> }
      ?.run(::createState)

    fun fetchOrCreateState(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime?,
    ): AggregateState<A> = when {
      pointInTime == null -> fetchOrCreateLatestState(assessment)
      pointInTime < assessment.createdAt -> throw InvalidTimestampException(pointInTime, "Timestamp cannot be before the assessment created date")
      else -> fetchOrCreateStateForExactPointInTime(assessment, pointInTime)
    }

    fun fetchOrCreateLatestState(assessment: AssessmentEntity): AggregateState<A> = aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, type.simpleName!!, clock.now())
      ?.let { it as AggregateEntity<A> }
      ?.run(::createState)
      ?: createStateForPointInTime(assessment, clock.now())

    fun fetchOrCreateStateForExactPointInTime(assessment: AssessmentEntity, pointInTime: LocalDateTime): AggregateState<A> = aggregateRepository.findByAssessmentAndTypeOnExactDate(assessment.uuid, type.simpleName!!, pointInTime)
      ?.let { it as AggregateEntity<A> }
      ?.run(::createState)
      ?: createStateForPointInTime(assessment, pointInTime)

    fun createStateForPointInTime(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime,
    ): AggregateState<A> = eventService
      .findAllForPointInTime(assessment.uuid, pointInTime)
      .sortedBy { it.id }
      .ifEmpty { null }
      ?.run(eventBus::handle)
      ?.get(type)
      .let { it ?: blankState(assessment) }
      .let { it as AggregateState<A> }
  }
}
