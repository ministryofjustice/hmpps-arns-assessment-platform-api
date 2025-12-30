package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import java.time.LocalDateTime

data class TimelineItem(
  val type: String,
  val createdAt: LocalDateTime,
  val data: Map<String, Any>,
)
