package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.model

import java.time.LocalDateTime

data class TimelineItem(
  val details: String = "",
  val timestamp: LocalDateTime,
)
