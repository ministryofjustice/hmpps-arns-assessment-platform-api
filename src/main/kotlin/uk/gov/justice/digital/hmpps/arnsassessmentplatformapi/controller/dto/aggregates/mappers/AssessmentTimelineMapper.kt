package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AggregateResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AssessmentTimelineResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentTimelineAggregate

@Component
class AssessmentTimelineMapper : AggregateResponseMapper {
  override val aggregateType = AssessmentTimelineAggregate.aggregateType

  override fun intoResponse(aggregate: Aggregate): AggregateResponse = (aggregate as AssessmentTimelineAggregate)
    .run { AssessmentTimelineResponse(aggregate.getTimeline()) }
}
