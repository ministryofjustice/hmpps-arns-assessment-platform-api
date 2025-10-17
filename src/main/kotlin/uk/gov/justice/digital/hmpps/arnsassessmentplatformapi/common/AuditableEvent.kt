package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common

import java.time.Instant

data class AuditableEvent(
  val who: String,
  val what: String,
  val `when`: Instant = Instant.now(),
  val service: String,
  val details: String,
)
