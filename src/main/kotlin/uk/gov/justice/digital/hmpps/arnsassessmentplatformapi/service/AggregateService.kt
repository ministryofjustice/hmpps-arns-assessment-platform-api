package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AggregateType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.Event

@Service
class AggregateService(
  val aggregateRepository: AggregateRepository,
) {
  private val aggregateRegistry: List<AggregateType> = listOf(
    AssessmentVersionAggregate,
    // Add more as needed
  )

  fun findAggregatesUpdatingOnEvent(event: Event): List<String> = aggregateRegistry
    .filter { event::class in it.updatesOn }
    .map { it.aggregateType }

  fun updateAggregate(assessment: AssessmentEntity, aggregateType: String, events: List<EventEntity>) {
    val latestAggregate = aggregateRepository.findLatestByAssessmentAndType(assessment.uuid, aggregateType)

    latestAggregate?.applyAll(events)
      ?: throw Error("Failed to update aggregate, no aggregate exists for the given type and uuid")

    aggregateRepository.save(latestAggregate)
  }
}
