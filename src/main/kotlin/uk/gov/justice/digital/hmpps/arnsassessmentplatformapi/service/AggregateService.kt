package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AggregateType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentTimelineAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import java.time.LocalDateTime

@Service
class AggregateService(
  val aggregateRepository: AggregateRepository,
  val eventRepository: EventRepository,
) {
  private val registry: Map<String, AggregateType> = listOf(
    AssessmentVersionAggregate,
    AssessmentTimelineAggregate,
    // Add more as needed
  ).associateBy { it.aggregateType }

  private fun typeFor(name: String): AggregateType = registry[name] ?: throw IllegalArgumentException("No aggregate is registered for type $name")
  fun getAggregateTypes() = registry.values

  private fun findOrCreate(
    assessment: AssessmentEntity,
    aggregateType: AggregateType,
    events: List<EventEntity>,
  ): AggregateEntity {
    val eventsFrom = events.minByOrNull { it.createdAt }?.createdAt ?: assessment.createdAt
    val eventsTo = events.maxByOrNull { it.createdAt }?.createdAt ?: LocalDateTime.now()
    val aggregate =
      aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType.aggregateType, eventsTo)
        ?.clone()
        ?: AggregateEntity(
          assessment = assessment,
          data = aggregateType.getInstance(),
          eventsFrom = eventsFrom,
          eventsTo = eventsTo,
        )

    events.forEach { event -> aggregate.apply(event) }
    return aggregate
  }

  fun createAggregate(assessment: AssessmentEntity, aggregateName: String): AggregateEntity {
    val events = eventRepository.findAllByAssessmentUuid(assessment.uuid)
    val aggregate = findOrCreate(assessment, typeFor(aggregateName), events)
    return aggregateRepository.save(aggregate)
  }

  fun createAggregateForPointInTime(
    assessment: AssessmentEntity,
    aggregateName: String,
    dateTime: LocalDateTime,
  ): AggregateEntity {
    val events = eventRepository.findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, dateTime)
    return findOrCreate(assessment, typeFor(aggregateName), events)
  }

  fun processEvents(assessment: AssessmentEntity, aggregateType: String, events: List<EventEntity>): AggregateEntity {
    return fetchLatestAggregateForType(assessment, aggregateType)?.let { latestAggregate ->
      events.sortedBy { it.createdAt }
        .fold(latestAggregate) { acc, event ->
          val eventApplied = acc.apply(event)

          if (eventApplied) {
            acc.eventsTo = event.createdAt
          }
          acc.updatedAt = LocalDateTime.now()

          if (acc.data.shouldCreate(event.data)) {
            return acc.clone()
              .also { cloned -> aggregateRepository.saveAll(listOf(acc, cloned)) }
          }

          return acc.also { aggregateRepository.save(acc) }
        }
    } ?: createAggregate(assessment, aggregateType)
  }

  fun fetchLatestAggregateForType(assessment: AssessmentEntity, aggregateType: String): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType, LocalDateTime.now())

  fun fetchAggregateForTypeOnDate(
    assessment: AssessmentEntity,
    aggregateType: String,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeOnExactDate(assessment.uuid, aggregateType, date)
}
