package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import java.util.UUID
import kotlin.collections.forEach

@Component
class CommandExecutorHelper(
  private val assessmentRepository: AssessmentRepository,
  private val eventRepository: EventRepository,
  private val aggregateService: AggregateService,
) {
  fun handleSave(events: List<EventEntity>) {
    if (events.isEmpty()) return

    eventRepository.saveAll(events)

    events.forEach { event ->
      aggregateService.findAggregatesUpdatingOnThisEvent(event.data)
        .forEach { aggregateType -> aggregateService.updateAggregate(event.assessment, aggregateType, events) }

      aggregateService.findAggregatesCreatingOnThisEvent(event.data)
        .forEach { aggregateType -> aggregateService.createAggregate(event.assessment, aggregateType) }
    }
  }

  fun fetchLatestAssessmentVersion(assessment: AssessmentEntity): AssessmentVersionAggregate? = aggregateService.fetchLatestAggregateForType(
    assessment,
    AssessmentVersionAggregate.aggregateType,
  )?.data as AssessmentVersionAggregate

  fun fetchAssessment(assessmentUuid: UUID): AssessmentEntity = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw IllegalArgumentException("Assessment not found: $assessmentUuid")

  fun createAssessment(): AssessmentEntity = assessmentRepository.save(AssessmentEntity())
}
