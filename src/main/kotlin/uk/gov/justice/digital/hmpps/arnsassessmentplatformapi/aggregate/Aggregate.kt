package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesAggregate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentAggregate::class, name = "AssessmentAggregate"),
  JsonSubTypes.Type(value = ValuesAggregate::class, name = "ValuesAggregate"),
)
interface Aggregate<A : Aggregate<A>> : AggregateView {
  fun clone(): A
}
