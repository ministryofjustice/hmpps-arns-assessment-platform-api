package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aggregate {
  fun apply(events: List<EventEntity>): Aggregate
}
