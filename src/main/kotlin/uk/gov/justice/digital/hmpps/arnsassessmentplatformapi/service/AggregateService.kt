package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AggregateType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentTimelineAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import java.time.Clock
import java.time.LocalDateTime

@Service
class AggregateService(
  private val aggregateRepository: AggregateRepository,
  private val eventRepository: EventRepository,
  private val clock: Clock,
  aggregateTypes: List<AggregateType> = listOf(
    AssessmentVersionAggregate,
    AssessmentTimelineAggregate,
  ),
) {
  private val registry: Map<String, AggregateType> =
    aggregateTypes.associateBy { it.aggregateType }

  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  private fun typeFor(name: String): AggregateType = registry[name] ?: throw IllegalArgumentException("No aggregate is registered for type $name")

  fun getAggregateTypes(): Collection<AggregateType> = registry.values

  @Transactional
  fun findOrCreate(
    assessment: AssessmentEntity,
    aggregateType: AggregateType,
    events: List<EventEntity>,
  ): AggregateEntity {
    if (events.isEmpty()) {
      return AggregateEntity(
        assessment = assessment,
        data = aggregateType.getInstance(),
        eventsFrom = assessment.createdAt,
        eventsTo = assessment.createdAt,
        updatedAt = now(),
      )
    }

    val eventsFrom = events.minBy { it.createdAt }.createdAt
    val eventsTo = events.maxBy { it.createdAt }.createdAt

    val base = aggregateRepository
      .findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType.aggregateType, eventsTo)
      ?.clone()
      ?: AggregateEntity(
        assessment = assessment,
        data = aggregateType.getInstance(),
        eventsFrom = eventsFrom,
        eventsTo = eventsFrom,
        updatedAt = now(),
      )

    events.sortedBy { it.createdAt }
      .forEach {
        val applied = base.apply(it)
        if (applied) base.eventsTo = it.createdAt
        base.updatedAt = now()
      }
    return base
  }

  @Transactional
  fun createAggregate(assessment: AssessmentEntity, aggregateName: String): AggregateEntity {
    val events = eventRepository.findAllByAssessmentUuid(assessment.uuid)
    return findOrCreate(assessment, typeFor(aggregateName), events)
      .run(aggregateRepository::save)
  }

  fun fetchLatestAggregateForType(assessment: AssessmentEntity, aggregateType: String): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType, now())

  fun fetchAggregateForTypeOnDate(
    assessment: AssessmentEntity,
    aggregateType: String,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeOnExactDate(assessment.uuid, aggregateType, date)

  @Transactional
  fun processEvents(
    assessment: AssessmentEntity,
    aggregateType: String,
    events: List<EventEntity>,
  ): AggregateEntity {
    val latest = fetchLatestAggregateForType(assessment, aggregateType)
      ?: return createAggregate(assessment, aggregateType)

    return events
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

        if (state.current.data.shouldCreate(event.data)) {
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
  }

  fun createAggregateForPointInTime(
    assessment: AssessmentEntity,
    aggregateName: String,
    dateTime: LocalDateTime,
  ): AggregateEntity {
    val aggType = typeFor(aggregateName)

    val base = aggregateRepository
      .findByAssessmentAndTypeBeforeDate(assessment.uuid, aggType.aggregateType, dateTime)
      ?.clone()
      ?: AggregateEntity(
        assessment = assessment,
        data = aggType.getInstance(),
        eventsFrom = assessment.createdAt,
        eventsTo = assessment.createdAt,
        updatedAt = now(),
      )

    val events = eventRepository
      .findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, dateTime)
      .sortedBy { it.createdAt }

    var lastAppliedAt: LocalDateTime? = null
    for (event in events) {
      if (base.apply(event)) lastAppliedAt = event.createdAt
    }

    if (lastAppliedAt != null) base.eventsTo = lastAppliedAt
    base.updatedAt = now()
    return base
  }
}
