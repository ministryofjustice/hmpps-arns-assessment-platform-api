package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import org.springframework.stereotype.Component

@Component
class AggregateTypeRegistry(
  private val aggregateTypes: Set<AggregateType> = setOf(
    AssessmentVersionAggregate.Companion,
    AssessmentTimelineAggregate.Companion,
  ),
) {
  fun getAggregates() = aggregateTypes.associateBy { it.aggregateType }
  fun getAggregateByName(name: String) = getAggregates().run { get(name) }
}
