package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.time.LocalDateTime

data class RedactedEvent(
  val eventType: String,
  val dateRedacted: LocalDateTime,
) : Event
