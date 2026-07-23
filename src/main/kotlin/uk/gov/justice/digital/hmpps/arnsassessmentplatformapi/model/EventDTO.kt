package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.exception.UndefinedPosition
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID

data class EventDTO(
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val position: Int,
  val data: Event,
) {
  companion object {
    fun from(event: EventEntity<*>) = EventDTO(
      uuid = event.uuid,
      createdAt = event.createdAt,
      position = event.position ?: throw UndefinedPosition("Event with UUID: ${event.uuid} does not have a valid position"),
      data = event.data,
    )
  }
}
