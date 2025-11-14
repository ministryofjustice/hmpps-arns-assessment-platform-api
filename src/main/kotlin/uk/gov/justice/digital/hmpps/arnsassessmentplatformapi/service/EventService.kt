package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

@Service
class EventService(
  private val eventRepository: EventRepository,
) {
  fun findAll(assessmentUuid: UUID) = eventRepository.findAllByAssessmentUuid(assessmentUuid)
  fun findAllForPointInTime(assessmentUuid: UUID, pointInTime: LocalDateTime) = eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(assessmentUuid, pointInTime)

  fun <E : Event> save(event: EventEntity<E>): EventEntity<E> = eventRepository.save(event)
}
