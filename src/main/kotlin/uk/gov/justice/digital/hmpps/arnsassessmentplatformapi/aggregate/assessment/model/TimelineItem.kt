package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model

import java.time.LocalDateTime

data class TimelineItem(
  val details: String = "",
  val timestamp: LocalDateTime,
)
