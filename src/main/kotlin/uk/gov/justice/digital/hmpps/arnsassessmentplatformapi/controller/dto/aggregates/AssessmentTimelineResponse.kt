package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.TimelineItem

data class AssessmentTimelineResponse(
  val timeline: List<TimelineItem>,
) : AggregateResponse
