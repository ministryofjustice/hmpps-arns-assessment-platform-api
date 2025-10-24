package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
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
  private val clock: Clock,
  private val collectionService: CollectionService,
) {
  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  @Transactional
  fun createAggregate(assessment: CollectionEntity, aggregateType: KClass<out Aggregate>): AggregateEntity = createAggregateForPointInTime(assessment, aggregateType, now())
    .run(aggregateRepository::save)

  fun fetchLatestAggregateBeforePointInTime(
    collectionUuid: UUID,
    aggregateType: KClass<out Aggregate>,
    pointInTime: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeBeforeDate(collectionUuid, aggregateType.simpleName!!, pointInTime)

  fun fetchLatestAggregate(collectionUuid: UUID, aggregateType: KClass<out Aggregate>): AggregateEntity? = fetchLatestAggregateBeforePointInTime(collectionUuid, aggregateType, now())

  fun fetchOrCreateAggregate(
    assessment: CollectionEntity,
    aggregateType: KClass<out Aggregate>,
    pointInTime: LocalDateTime?,
  ): AggregateEntity = if (pointInTime != null) {
    fetchAggregateForExactPointInTime(assessment, aggregateType, pointInTime)
      ?: createAggregateForPointInTime(assessment, aggregateType, pointInTime)
  } else {
    fetchLatestAggregate(assessment.uuid, aggregateType)
      ?: createAggregateForPointInTime(assessment, aggregateType, now())
  }

  fun fetchAggregateForExactPointInTime(
    assessment: CollectionEntity,
    aggregateType: KClass<out Aggregate>,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeOnExactDate(assessment.uuid, aggregateType.simpleName!!, date)

  fun processEvents(
    collectionUuid: UUID,
    aggregateType: KClass<out Aggregate>,
    events: List<EventEntity>,
  ): AggregateEntity {
    val latest = fetchLatestAggregate(collectionUuid, aggregateType)
      ?: collectionService.findByUuid(collectionUuid).let { collection ->
        AggregateEntity(
          collection = collection,
          data = aggregateType.createInstance(),
          eventsFrom = collection.createdAt,
          eventsTo = collection.createdAt,
          updatedAt = LocalDateTime.now(),
        )
          .apply { eventService.findAllByCollectionUuid(collection.uuid).forEach { event -> apply(event) } }
      }

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
    collection: CollectionEntity,
    aggregateType: KClass<out Aggregate>,
    pointInTime: LocalDateTime,
  ): AggregateEntity {
    val events = eventService
      .findAllByCollectionUuidAndCreatedAtBefore(collection.uuid, pointInTime)
      .sortedBy { it.createdAt }

    val base = events.maxByOrNull { it.createdAt }?.let { latestEvent ->
      fetchLatestAggregateBeforePointInTime(collection.uuid, aggregateType, latestEvent.createdAt)
        ?.clone()
    } ?: AggregateEntity(
      collection = collectionService.findRoot(collection),
      data = aggregateType.createInstance(),
      eventsFrom = collection.createdAt,
      eventsTo = collection.createdAt,
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
