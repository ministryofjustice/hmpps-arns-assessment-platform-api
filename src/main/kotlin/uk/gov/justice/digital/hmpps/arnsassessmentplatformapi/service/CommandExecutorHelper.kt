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
  fun handleSave(assessmentUUID: UUID, events: List<EventEntity>) {
    if (events.isEmpty()) return

    val assessment = fetchAssessment(assessmentUUID)
    val savedEvents = eventRepository.saveAll(events)
    val eventTypes = savedEvents.mapTo(mutableSetOf()) { it.data::class }

    aggregateService.getAggregateTypes()
      .asSequence()
      .filter { it.createsOn.any(eventTypes::contains) || it.updatesOn.any(eventTypes::contains) }
      .forEach { aggregateService.processEvents(assessment, it.aggregateType, events) }
  }

  fun fetchAssessment(assessmentUuid: UUID): AssessmentEntity = assessmentRepository.findByUuid(assessmentUuid)
    ?: throw IllegalArgumentException("Assessment not found: $assessmentUuid")

  fun createAssessment(): AssessmentEntity = assessmentRepository.save(AssessmentEntity())
}
