package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class AssessmentAnswersUpdatedEvent(
  val added: Map<String, Value>,
  val removed: List<String>,
) : Event {
  override fun toTimeLineItemData(): Map<String, Any> = buildMap {
    put("added", added)
    put("removed", removed)
  }
}
