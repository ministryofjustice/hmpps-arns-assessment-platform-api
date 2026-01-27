package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class AssessmentPropertiesUpdatedEvent(
  val added: Map<String, Value>,
  val removed: List<String>,

) : Event {
  override fun toTimeLineItemData(): Map<String, Any> = mapOf(
    "added" to added.keys,
    "removed" to removed,
  )
}
