package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AssessmentTimelineResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentTimelineAggregate

@Component
class AssessmentTimelineMapper : AggregateResponseMapper<AssessmentTimelineAggregate> {
  override val aggregateType = AssessmentTimelineAggregate::class

  override fun createResponseFrom(aggregate: AssessmentTimelineAggregate) = AssessmentTimelineResponse(aggregate.getTimeline())
}
