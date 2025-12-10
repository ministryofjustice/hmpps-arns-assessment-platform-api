package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentAggregate::class, name = "AssessmentAggregate"),
)
interface Aggregate<A : Aggregate<A>> : AggregateView {
  fun clone(): A
}
