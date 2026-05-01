package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
class EventService(
  private val eventRepository: EventRepository,
) {
  fun findAllForPointInTime(
    assessmentUuid: UUID,
    pointInTime: LocalDateTime,
  ) = eventRepository.findAllByAssessmentUuidAndCreatedAtIsLessThanEqual(assessmentUuid, pointInTime)

  fun findAllBetween(
    assessmentUuid: UUID,
    from: LocalDateTime,
    to: LocalDateTime,
  ) = eventRepository.findAllByAssessmentUuidAndCreatedAtGreaterThanAndCreatedAtLessThanEqual(assessmentUuid, from, to)

  fun saveAll(entities: List<EventEntity<*>>): List<EventEntity<*>> {
    entities.groupBy { it.assessment.uuid }
      .forEach { (assessmentUuid, events) ->
        val maxPosition = eventRepository.findTopByAssessmentUuidOrderByPositionDesc(assessmentUuid)?.position ?: -1
        events.forEachIndexed { index, entity -> entity.position = maxPosition + 1 + index }
      }
    return eventRepository.saveAll(entities)
  }

  fun <E : Event> save(event: EventEntity<E>): EventEntity<E> = eventRepository.save(event)

  fun softDelete(assessmentUuid: UUID, from: LocalDateTime) {
    eventRepository.findAllByAssessmentUuidAndCreatedAtGreaterThanEqual(assessmentUuid, from).map {
      it.apply { deleted = true }
    }.run(eventRepository::saveAll)
  }

  fun findAssessmentsSoftDeletedSince(
    assessmentType: String,
    since: LocalDateTime,
  ) = eventRepository.findAssessmentsSoftDeletedSince(assessmentType, since)
}
