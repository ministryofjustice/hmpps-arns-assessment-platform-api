package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.model.TimelineItem

data class AssessmentTimelineQueryResult(
  val timeline: List<TimelineItem>,
) : QueryResult
