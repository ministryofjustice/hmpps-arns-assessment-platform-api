package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.TimelineItem

@JsonTypeName("AssessmentTimelineResult")
data class AssessmentTimelineResult(
  val timeline: List<TimelineItem>,
) : QueryResult
