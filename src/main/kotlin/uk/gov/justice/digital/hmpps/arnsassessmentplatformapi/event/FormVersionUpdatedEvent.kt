package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

data class FormVersionUpdatedEvent(
  val version: String,
) : Event
