package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import com.fasterxml.jackson.annotation.JsonTypeName

private const val EVENT_TYPE = "FORM_VERSION_UPDATED"

@JsonTypeName(EVENT_TYPE)
data class FormVersionUpdatedEvent(
  val version: String,
) : Event {
  companion object : EventType {
    override val eventType = EVENT_TYPE
  }
}
