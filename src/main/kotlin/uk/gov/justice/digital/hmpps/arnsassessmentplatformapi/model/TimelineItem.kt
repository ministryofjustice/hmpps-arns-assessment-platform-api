package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.User
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
      user = User.Companion.from(entity.user),
      event = entity.eventType,
      data = entity.data,
      customType = entity.customType,
      customData = entity.customData,
    )
  }
}
