package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Timeline

data class AssessmentTimelineQueryResult(
  val timeline: Timeline,
) : QueryResult
