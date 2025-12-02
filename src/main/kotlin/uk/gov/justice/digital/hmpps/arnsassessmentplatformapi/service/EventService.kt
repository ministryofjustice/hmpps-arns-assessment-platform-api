package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

@Service
class EventService(
  private val eventRepository: EventRepository,
) {
  private val parentEvent = ThreadLocal<EventEntity<GroupEvent>?>()
  fun setParentEvent(event: EventEntity<GroupEvent>) = parentEvent.set(event)
  fun clearParentEvent() = parentEvent.remove()

  fun findAllForPointInTime(assessmentUuid: UUID, pointInTime: LocalDateTime) = eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqualAndParentIsNull(assessmentUuid, pointInTime)

  fun <E : Event> save(event: EventEntity<E>): EventEntity<E> = eventRepository.save(event.apply { parent = parentEvent.get() })
}
