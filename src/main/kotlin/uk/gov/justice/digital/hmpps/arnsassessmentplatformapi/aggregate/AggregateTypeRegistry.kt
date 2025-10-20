package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

@Component
class AggregateTypeRegistry(
  private val aggregateTypes: Set<KClass<out Aggregate>> = setOf(
    AssessmentVersionAggregate::class,
    AssessmentTimelineAggregate::class,
  ),
) {
  fun getAggregates() = aggregateTypes.associateWith { it.companionObjectInstance as AggregateType }
}
