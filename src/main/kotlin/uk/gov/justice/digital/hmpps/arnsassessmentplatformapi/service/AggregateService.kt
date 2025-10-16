package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateTypeRegistry
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AggregateNotRegisteredException
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class AggregateService(
  private val aggregateRepository: AggregateRepository,
  private val eventService: EventService,
  private val registry: AggregateTypeRegistry,
  private val clock: Clock,
) {
  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  private fun typeFor(name: String): AggregateType = registry.getAggregateByName(name)
    ?: throw AggregateNotRegisteredException("No aggregate is registered for type: $name, registered types: ${getAggregateTypes().joinToString { it.aggregateType }}")

  fun getAggregateTypes(): Set<AggregateType> = registry.getAggregates().values.toSet()

  @Transactional
  fun createAggregate(assessment: AssessmentEntity, aggregateName: String): AggregateEntity = createAggregateForPointInTime(assessment, aggregateName, now())
    .run(aggregateRepository::save)

  fun fetchLatestAggregateBeforePointInTime(
    assessmentUuid: UUID,
    aggregateName: String,
    pointInTime: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeBeforeDate(assessmentUuid, aggregateName, pointInTime)

  fun fetchLatestAggregate(assessmentUuid: UUID, aggregateName: String): AggregateEntity? = fetchLatestAggregateBeforePointInTime(assessmentUuid, aggregateName, now())

  fun fetchOrCreateAggregate(
    assessment: AssessmentEntity,
    aggregateName: String,
    pointInTime: LocalDateTime?,
  ): AggregateEntity = if (pointInTime != null) {
    fetchAggregateForExactPointInTime(assessment, aggregateName, pointInTime)
      ?: createAggregateForPointInTime(assessment, aggregateName, pointInTime).run(aggregateRepository::save)
  } else {
    fetchLatestAggregate(assessment.uuid, aggregateName)
      ?: createAggregateForPointInTime(assessment, aggregateName, now()).run(aggregateRepository::save)
  }

  fun fetchAggregateForExactPointInTime(
    assessment: AssessmentEntity,
    aggregateName: String,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeOnExactDate(assessment.uuid, aggregateName, date)

  fun processEvents(
    assessment: AssessmentEntity,
    aggregateType: String,
    events: List<EventEntity>,
  ): AggregateEntity {
    val latest = fetchLatestAggregate(assessment.uuid, aggregateType)
      ?: AggregateEntity.getDefault(assessment, typeFor(aggregateType).getInstance())
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
    aggregateName: String,
    pointInTime: LocalDateTime,
  ): AggregateEntity {
    val aggregateType = typeFor(aggregateName)

    val events = eventService
      .findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, pointInTime)
      .sortedBy { it.createdAt }

    val base = events.maxByOrNull { it.createdAt }?.let { latestEvent ->
      fetchLatestAggregateBeforePointInTime(assessment.uuid, aggregateName, latestEvent.createdAt)
        ?.clone()
    } ?: AggregateEntity(
      assessment = assessment,
      data = aggregateType.getInstance(),
      eventsFrom = assessment.createdAt,
      eventsTo = assessment.createdAt,
      updatedAt = now(),
    )

    var lastAppliedAt: LocalDateTime? = null
    for (event in events) {
      if (base.apply(event)) lastAppliedAt = event.createdAt
    }

    if (lastAppliedAt != null) base.eventsTo = lastAppliedAt
    base.updatedAt = now()
    return base
  }
}
