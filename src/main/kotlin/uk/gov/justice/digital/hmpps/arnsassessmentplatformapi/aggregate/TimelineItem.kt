package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import java.time.LocalDateTime

data class TimelineItem(
  val details: String = "",
  val timestamp: LocalDateTime,
)
