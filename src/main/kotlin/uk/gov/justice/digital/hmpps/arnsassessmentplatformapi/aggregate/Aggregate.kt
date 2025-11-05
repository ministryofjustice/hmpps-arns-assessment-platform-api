package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
interface Aggregate<A : Aggregate<A>> {
  fun clone(): A
}
