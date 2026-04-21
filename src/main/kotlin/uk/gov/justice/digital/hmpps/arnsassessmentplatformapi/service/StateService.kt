package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBusFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContextFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AggregateTypeNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.InvalidTimestampException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Service
class StateService(
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  @param:Lazy private val eventBusFactory: EventBusFactory,
  @param:Lazy private val persistenceContextFactory: PersistenceContextFactory,
  private val clock: Clock,
  private val assessmentVersionCacheService: AssessmentVersionCacheService,
) {
  fun persist(state: MutableMap<UUID, State>) {
    state.flatMap { (assessmentUuid, assessmentState) ->
      assessmentState.values.flatMap { aggregateState ->
        val maxPosition = aggregateRepository.findTopByAssessmentUuidAndDataTypeOrderByPositionDesc(
          assessmentUuid,
          aggregateState.type.simpleName ?: throw IllegalStateException("Aggregate type ${aggregateState.type} is nameless"),
        )?.position ?: -1
        aggregateState.aggregates.mapIndexed { index, aggregate -> aggregate.apply { position = maxPosition + 1 + index } }
      }
    }.run(aggregateRepository::saveAll)

    state.keys.forEach(assessmentVersionCacheService::evictLatestAfterCommit)
  }

  fun rebuildFromEvents(
    assessment: AssessmentEntity,
    pointInTime: LocalDateTime?,
  ): State? = eventService
    .findAllForPointInTime(assessment.uuid, pointInTime ?: clock.now())
    .sortedBy { it.position }
    .ifEmpty { null }
    ?.let { events ->
      val persistenceContext = persistenceContextFactory.create().apply {
        state[assessment.uuid] = mutableMapOf(
          AssessmentAggregate::class to stateForType(AssessmentAggregate::class).blankState(assessment),
        )
      }
      val eventBus = eventBusFactory.create(persistenceContext)
      eventBus.handle(events)
      eventBus.getState()
    }?.get(assessment.uuid)

  fun delete(assessmentUuid: UUID) {
    aggregateRepository.deleteByAssessmentUuid(assessmentUuid)
  }

  fun stateForType(type: KClass<out Aggregate<*>>) = StateForType(type)

  inner class StateForType<A : Aggregate<A>>(
    private val type: KClass<A>,
  ) {
    fun fetchOrCreateState(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime?,
    ): AggregateState<A> = when {
      pointInTime == null -> fetchOrCreateLatestState(assessment)
      pointInTime < assessment.createdAt -> throw InvalidTimestampException(pointInTime, "Timestamp cannot be before the assessment created date")
      else -> createPointInTimeStateFromAggregate(assessment, pointInTime)
    }

    private fun createState(aggregateEntity: AggregateEntity<A>): AggregateState<A> = when (type) {
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

    private fun fetchOrCreateLatestState(assessment: AssessmentEntity): AggregateState<A> = aggregateRepository.findTopByAssessmentUuidAndDataTypeAndEventsToLessThanEqualOrderByPositionDesc(assessment.uuid, type.simpleName!!, clock.now())
      ?.let { it as AggregateEntity<A> }
      ?.run(::createState)
      ?: createPointInTimeStateFromEvents(assessment, clock.now())

    private fun createPointInTimeStateFromAggregate(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime,
    ): AggregateState<A> {
      val aggregate = aggregateRepository.findTopByAssessmentUuidAndDataTypeAndEventsToLessThanEqualOrderByPositionDesc(assessment.uuid, type.simpleName!!, pointInTime)
        ?: return createPointInTimeStateFromEvents(assessment, pointInTime)

      val aggregateState = createState(aggregate as AggregateEntity<A>)

      if (aggregate.eventsTo == pointInTime) return aggregateState

      val eventsBetween = eventService
        .findAllBetween(assessment.uuid, aggregate.eventsTo, pointInTime)
        .sortedBy { it.position }

      if (eventsBetween.isEmpty()) return aggregateState

      val persistenceContext = persistenceContextFactory.create().apply {
        state[assessment.uuid] = mutableMapOf(type to aggregateState)
      }

      val newAggregateState = eventBusFactory.create(persistenceContext)
        .apply { handle(eventsBetween) }
        .getState()[assessment.uuid]?.get(type) as? AggregateState<A>

      return newAggregateState ?: aggregateState
    }

    private fun createPointInTimeStateFromEvents(
      assessment: AssessmentEntity,
      pointInTime: LocalDateTime,
    ): AggregateState<A> = rebuildFromEvents(assessment, pointInTime)
      ?.get(type)
      .let { it ?: blankState(assessment) }
      .let { it as AggregateState<A> }
  }
}
