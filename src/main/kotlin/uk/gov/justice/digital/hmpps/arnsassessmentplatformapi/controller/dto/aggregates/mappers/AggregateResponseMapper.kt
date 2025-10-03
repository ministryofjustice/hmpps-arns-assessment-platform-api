package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AggregateResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate

interface AggregateResponseMapper {
  val aggregateType: String
  fun intoResponse(aggregate: Aggregate): AggregateResponse
}
