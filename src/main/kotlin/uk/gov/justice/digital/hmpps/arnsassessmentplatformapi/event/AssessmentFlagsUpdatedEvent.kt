package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

data class AssessmentFlagsUpdatedEvent(
  val flags: List<String>,
) : Event
