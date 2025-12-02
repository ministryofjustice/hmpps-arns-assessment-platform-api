package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentAggregate::class, name = "AssessmentAggregate"),
)
sealed interface Aggregate<A : Aggregate<A>> {
  fun clone(): A
}
