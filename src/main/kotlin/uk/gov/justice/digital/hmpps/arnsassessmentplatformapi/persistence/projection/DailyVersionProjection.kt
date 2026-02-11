package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.projection

import java.time.LocalDateTime
import java.util.UUID

interface DailyVersionProjection {
  val createdAt: LocalDateTime
  val updatedAt: LocalDateTime
  val lastTimelineUuid: UUID
}
