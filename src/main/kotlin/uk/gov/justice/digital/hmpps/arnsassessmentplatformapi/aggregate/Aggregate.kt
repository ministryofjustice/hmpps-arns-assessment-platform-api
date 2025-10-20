package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.reflect.KClass

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aggregate {
  var numberOfEventsApplied: Long
  fun apply(event: EventEntity): Boolean
  fun clone(): Aggregate
  fun shouldCreate(event: KClass<out Event>): Boolean
  fun shouldUpdate(event: KClass<out Event>): Boolean
}
