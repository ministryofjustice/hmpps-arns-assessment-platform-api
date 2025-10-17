package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import com.fasterxml.jackson.annotation.JsonTypeName

private const val EVENT_TYPE = "ANSWERS_UPDATED"

@JsonTypeName(EVENT_TYPE)
data class AnswersUpdatedEvent(
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event {
  companion object : EventType {
    override val eventType = EVENT_TYPE
  }
}
