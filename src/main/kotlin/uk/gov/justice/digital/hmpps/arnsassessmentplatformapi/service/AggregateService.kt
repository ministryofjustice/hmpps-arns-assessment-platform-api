package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AggregateType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.Event
import java.time.LocalDateTime

@Service
class AggregateService(
  val aggregateRepository: AggregateRepository,
  val eventRepository: EventRepository,
) {
  private val aggregateRegistry: List<AggregateType> = listOf(
    AssessmentVersionAggregate,
    // Add more as needed
  )

  fun findAggregatesCreatingOnThisEvent(event: Event): List<String> = aggregateRegistry
    .filter { it.createsOn.contains(event::class) }
    .map { it.aggregateType }

  fun findAggregatesUpdatingOnThisEvent(event: Event): List<String> = aggregateRegistry
    .filter { it.updatesOn.contains(event::class) }
    .map { it.aggregateType }

  fun createAggregate(assessment: AssessmentEntity, aggregateType: String) {
    eventRepository.findAllByAssessmentUuid(assessment.uuid).let { events ->
      aggregateRegistry.find { it.aggregateType == aggregateType }?.run {
        val eventsTo = LocalDateTime.now()
        val aggregate =
          aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType, eventsTo)
            ?.clone()
            ?: AggregateEntity(
              assessment = assessment,
              data = getInstance(),
              eventsFrom = events.minByOrNull { it.createdAt }?.createdAt ?: assessment.createdAt,
              eventsTo = eventsTo,
            )

        aggregate.apply { applyAll(events) }
          .also(aggregateRepository::save)
      }
    }
  }

  fun createAggregateForPointInTime(
    assessment: AssessmentEntity,
    aggregateType: String,
    dateTime: LocalDateTime,
    shouldPersist: Boolean = false,
  ): AggregateEntity = eventRepository.findAllByAssessmentUuidAndCreatedAtBefore(assessment.uuid, dateTime).let { events ->
    aggregateRegistry.find { it.aggregateType == aggregateType }?.run {
      val aggregate =
        aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType, dateTime)?.clone()
          ?: AggregateEntity(
            assessment = assessment,
            data = getInstance(),
            eventsFrom = events.minByOrNull { it.createdAt }?.createdAt ?: assessment.createdAt,
            eventsTo = dateTime,
          )

      aggregate.apply { applyAll(events) }
      if (shouldPersist) {
        aggregate.run(aggregateRepository::save)
      } else {
        aggregate
      }
    } as AggregateEntity
  }

  fun updateAggregate(assessment: AssessmentEntity, aggregateType: String, events: List<EventEntity>) {
    val latestAggregate = fetchLatestAggregateForType(assessment, aggregateType)?.applyAll(events)
      ?: throw Error("Failed to update aggregate, no aggregate exists for the given type and uuid")

    aggregateRepository.save(latestAggregate)
  }

  fun fetchLatestAggregateForType(assessment: AssessmentEntity, aggregateType: String): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeBeforeDate(assessment.uuid, aggregateType, LocalDateTime.now())

  fun fetchAggregateForTypeOnDate(
    assessment: AssessmentEntity,
    aggregateType: String,
    date: LocalDateTime,
  ): AggregateEntity? = aggregateRepository.findByAssessmentAndTypeOnExactDate(assessment.uuid, aggregateType, date)
}
