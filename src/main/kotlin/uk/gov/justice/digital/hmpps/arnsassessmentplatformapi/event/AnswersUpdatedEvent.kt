package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

data class AnswersUpdatedEvent(
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event
