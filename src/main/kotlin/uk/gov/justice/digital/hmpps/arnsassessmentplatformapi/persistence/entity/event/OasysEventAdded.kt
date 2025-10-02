package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName

private const val EVENT_TYPE = "OASYS_EVENT"

@JsonTypeName(EVENT_TYPE)
data class OasysEventAdded(
  val tag: String,
) : Event {
  companion object : EventType {
    override val eventType = EVENT_TYPE
  }
}
