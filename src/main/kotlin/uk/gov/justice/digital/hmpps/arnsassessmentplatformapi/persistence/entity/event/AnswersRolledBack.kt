package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime

private const val EVENT_TYPE = "ANSWERS_ROLLED_BACK"

@JsonTypeName(EVENT_TYPE)
data class AnswersRolledBack(
  val rolledBackTo: LocalDateTime,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event {
  companion object : EventType {
    override val eventType = EVENT_TYPE
  }
}
