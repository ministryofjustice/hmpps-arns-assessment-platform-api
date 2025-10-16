package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import com.fasterxml.jackson.annotation.JsonTypeName

private const val EVENT_TYPE = "ASSESSMENT_CREATED"

@JsonTypeName(EVENT_TYPE)
class AssessmentCreated : Event {
  companion object : EventType {
    override val eventType = EVENT_TYPE
  }
}
