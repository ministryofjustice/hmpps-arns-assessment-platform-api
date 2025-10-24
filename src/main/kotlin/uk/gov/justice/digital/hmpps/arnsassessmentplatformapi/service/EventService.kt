package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

@Service
class EventService(
  private val eventRepository: EventRepository,
) {
  fun findAllByCollectionUuid(uuid: UUID) = eventRepository.findAllByCollectionUuid(uuid)
  fun findAllByCollectionUuidAndCreatedAtBefore(uuid: UUID, pointInTime: LocalDateTime) = eventRepository.findAllByCollectionUuidAndCreatedAtBefore(uuid, pointInTime)

  fun saveAll(events: List<EventEntity>) {
    eventRepository.saveAll(events)
  }
}
