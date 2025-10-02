package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers.AggregateResponseMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate

@Component
class AggregateResponseMapperRegistry(mappers: List<AggregateResponseMapper>) {
  private val byType = mappers.associateBy { it.aggregateType }

  fun intoResponse(type: String, aggregate: Aggregate): AggregateResponse = byType[type]?.intoResponse(aggregate)
    ?: error("No mapper for type=$type")
}
