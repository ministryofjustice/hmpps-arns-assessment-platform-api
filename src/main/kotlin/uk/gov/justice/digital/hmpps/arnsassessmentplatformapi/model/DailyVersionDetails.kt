package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.projection.DailyVersionProjection
import java.time.LocalDateTime
import java.util.UUID

data class DailyVersionDetails(
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val lastTimelineItemUuid: UUID,
) {
  companion object {
    fun from(projection: DailyVersionProjection) = with(projection) {
      DailyVersionDetails(
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastTimelineItemUuid = lastTimelineUuid,
      )
    }
  }
}
