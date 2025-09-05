package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.Event

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aggregate {
  var numberOfEventsApplied: Long
  fun apply(event: EventEntity): Boolean
  fun clone(): Aggregate
  fun shouldCreate(event: Event): Boolean
  fun shouldUpdate(event: Event): Boolean
}
