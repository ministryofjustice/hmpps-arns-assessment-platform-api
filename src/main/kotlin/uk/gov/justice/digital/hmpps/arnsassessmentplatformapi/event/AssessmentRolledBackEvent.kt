package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.time.LocalDateTime

data class AssessmentRolledBackEvent(
  val rolledBackTo: LocalDateTime,

) : Event {
  override fun toTimeLineItemData(): Map<String, Any> = mapOf(
    "rolledBackTo" to rolledBackTo,
  )
}
