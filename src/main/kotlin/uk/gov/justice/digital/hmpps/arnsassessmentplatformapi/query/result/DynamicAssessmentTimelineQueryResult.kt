package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import java.time.LocalDateTime

data class TimelineItem(
  val timestamp: LocalDateTime,
  val user: User,
  val event: String,
  val data: MutableMap<String, Any> = mutableMapOf(),
)

data class PageInfo(
  val pageNumber: Int,
  val totalPages: Int,
)

class DynamicAssessmentTimelineQueryResult(
  val timeline: List<TimelineItem>,
  val pageInfo: PageInfo? = null,
) : QueryResult
