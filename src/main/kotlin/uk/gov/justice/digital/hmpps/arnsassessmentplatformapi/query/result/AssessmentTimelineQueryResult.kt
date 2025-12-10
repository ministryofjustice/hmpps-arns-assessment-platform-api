package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.TimelineView

data class AssessmentTimelineQueryResult(
  val timeline: TimelineView,
) : QueryResult
