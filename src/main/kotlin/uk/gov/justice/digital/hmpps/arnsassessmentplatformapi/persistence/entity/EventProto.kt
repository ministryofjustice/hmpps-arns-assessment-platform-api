package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import java.time.LocalDateTime

data class EventProto<E : Event>(
  val createdAt: LocalDateTime,
  val user: UserDetailsEntity,
  val assessment: AssessmentEntity,
  val data: E,
) {
  companion object {
    fun from(eventEntity: EventEntity<*>): EventProto<*> {
      return EventProto(
        createdAt = eventEntity.createdAt,
        user = eventEntity.user,
        assessment = eventEntity.assessment,
        data = eventEntity.data,
      )
    }
  }
}
