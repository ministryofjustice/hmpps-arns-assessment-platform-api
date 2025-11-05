package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

class AssessmentCreatedEvent(
  val properties: Map<String, List<String>>,
) : Event
