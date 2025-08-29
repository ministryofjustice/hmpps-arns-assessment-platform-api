package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.util.UUID

@Component
class CommandExecutorHelper(
  private val assessmentRepository: AssessmentRepository,
  private val eventRepository: EventRepository,
  private val aggregateService: AggregateService,
) {
  fun handleSave(events: List<EventEntity>) {
    if (events.isEmpty()) return

    eventRepository.saveAll(events)
      .forEach { event ->
        aggregateService.findAggregatesUpdatingOnThisEvent(event.data)
          .forEach { aggregateType -> aggregateService.updateAggregate(event.assessment, aggregateType, events) }

        aggregateService.findAggregatesCreatingOnThisEvent(event.data)
          .forEach { aggregateType -> aggregateService.createAggregate(event.assessment, aggregateType) }
      }
  }

  fun fetchAssessment(assessmentUuid: UUID): AssessmentEntity = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw IllegalArgumentException("Assessment not found: $assessmentUuid")

  fun createAssessment(): AssessmentEntity = assessmentRepository.save(AssessmentEntity())
}
