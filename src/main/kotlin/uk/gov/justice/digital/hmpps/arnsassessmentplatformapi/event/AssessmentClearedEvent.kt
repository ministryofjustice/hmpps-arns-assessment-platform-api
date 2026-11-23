package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

data class AssessmentClearedEvent(
  val properties: List<String>,
  val collections: List<String>,
) : Event
