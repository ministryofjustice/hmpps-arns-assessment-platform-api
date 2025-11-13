package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Event {
  val timeline: Timeline?
}
