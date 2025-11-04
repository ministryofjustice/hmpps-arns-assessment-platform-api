package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
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

  @Transactional
  fun createAggregate(assessment: AssessmentEntity): AggregateEntity = createAggregateForPointInTime(assessment, now())
    .run(aggregateRepository::save)

  fun fetchLatestAggregateBeforePointInTime(
    assessmentUuid: UUID,
    pointInTime: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentBeforeDate(assessmentUuid, pointInTime)

  fun fetchLatestAggregate(assessmentUuid: UUID): AggregateEntity? = fetchLatestAggregateBeforePointInTime(assessmentUuid, now())

  fun fetchOrCreateAggregate(
    assessment: AssessmentEntity,
    pointInTime: LocalDateTime?,
  ): AggregateEntity = if (pointInTime != null) {
    fetchAggregateForExactPointInTime(assessment, pointInTime)
      ?: createAggregateForPointInTime(assessment, pointInTime).run(aggregateRepository::save)
  } else {
    fetchLatestAggregate(assessment.uuid)
      ?: createAggregateForPointInTime(assessment, now()).run(aggregateRepository::save)
  }

  fun fetchAggregateForExactPointInTime(
    assessment: AssessmentEntity,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentOnExactDate(assessment.uuid, date)

  fun processEvents(
    assessment: AssessmentEntity,
    events: List<EventEntity>,
  ): AggregateEntity {
    val latest = fetchLatestAggregate(assessment.uuid)
      ?: AggregateEntity(
        assessment = assessment,
        data = AssessmentAggregate(),
        eventsFrom = assessment.createdAt,
        eventsTo = assessment.createdAt,
        updatedAt = now(),
      )
        .apply { eventService.findAllByAssessmentUuid(assessment.uuid).forEach { event -> apply(event) } }

    events
      .sortedBy { it.createdAt }
      .fold(
        object {
          var current = latest
          var isDirty = false
          val toPersist = mutableListOf<AggregateEntity>()
        },
      ) { state, event ->
        val applied = state.current.apply(event)
        if (applied) {
          state.apply {
            current.eventsTo = event.createdAt
            isDirty = true
          }
        }
        state.current.updatedAt = now()

        if (state.current.data.shouldCreate(event.data::class)) {
          if (state.isDirty) state.toPersist += state.current

          val cloned = state.current.clone().also { it.updatedAt = now() }
          state.apply {
            toPersist += cloned
            current = cloned
            isDirty = false
          }
        }

        state
      }.run {
        if (isDirty) toPersist += current
        if (toPersist.isNotEmpty()) aggregateRepository.saveAll(toPersist)
        current
      }

    return latest
  }

  fun createAggregateForPointInTime(
    assessment: AssessmentEntity,
    pointInTime: LocalDateTime,
  ): AggregateEntity {
    val events = eventService
      .findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
      .sortedBy { it.createdAt }

    val base = events.maxByOrNull { it.createdAt }?.let { latestEvent ->
      fetchLatestAggregateBeforePointInTime(assessment.uuid, latestEvent.createdAt)
        ?.clone()
    } ?: AggregateEntity(
      assessment = assessment,
      data = AssessmentAggregate(),
      eventsFrom = assessment.createdAt,
      eventsTo = assessment.createdAt,
      updatedAt = now(),
    )

    var lastAppliedAt: LocalDateTime? = null

    return events.fold(base) { acc, event -> eventBus.handle(event, acc) }
    events.forEach { event -> eventBus.handle(event, base) }
    for (event in events) {

      if (base.apply(event)) lastAppliedAt = event.createdAt
    }

    if (lastAppliedAt != null) base.eventsTo = lastAppliedAt
    base.updatedAt = now()
    return base
  }

  fun fetchLatestState(assessment: AssessmentEntity) = let {
    fetchLatestAggregate(assessment.uuid)
      ?: AggregateEntity(
        assessment = assessment,
        data = AssessmentAggregate(),
        eventsFrom = assessment.createdAt,
        eventsTo = assessment.createdAt,
        updatedAt = now(),
      )
  }.run(::AssessmentState)

  fun persistState(assessmentState: AssessmentState) {
    aggregateRepository.saveAll(assessmentState.aggregates)
  }
}
