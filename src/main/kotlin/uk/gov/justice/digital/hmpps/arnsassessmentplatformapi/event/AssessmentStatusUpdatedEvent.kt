package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

data class AssessmentStatusUpdatedEvent(
  val status: String,
) : Event
