package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import java.time.LocalDateTime
import java.util.UUID

data class TimelineItem(
  val timestamp: LocalDateTime,
  val user: User,
  val assessment: UUID,
  val event: String,
  var data: Map<String, Any> = mapOf(),
  var customType: String? = null,
  var customData: Map<String, Any>? = null,
) {
  companion object {
    fun from(entity: TimelineEntity) = TimelineItem(
      timestamp = entity.createdAt,
      user = User.from(entity.user),
      assessment = entity.assessment.uuid,
      event = entity.eventType,
      data = entity.data,
      customType = entity.customType,
      customData = entity.customData,
    )
  }
}
