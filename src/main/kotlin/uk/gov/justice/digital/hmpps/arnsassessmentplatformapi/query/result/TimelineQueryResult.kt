package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import java.time.LocalDateTime

data class TimelineItem(
  val timestamp: LocalDateTime,
  val user: User,
  val event: String,
  var data: Map<String, Any> = mapOf(),
  var customType: String? = null,
  var customData: Map<String, Any>? = mapOf(),
) {
  companion object {
    fun from(entity: TimelineEntity) = TimelineItem(
      timestamp = entity.createdAt,
      user = User.from(entity.user),
      event = entity.eventType,
      data = entity.data,
      customType = entity.customType,
      customData = entity.customData,
    )
  }
}

data class PageInfo(
  val pageNumber: Int,
  val totalPages: Int,
)

class TimelineQueryResult(
  val timeline: List<TimelineItem>,
  val pageInfo: PageInfo? = null,
) : QueryResult
