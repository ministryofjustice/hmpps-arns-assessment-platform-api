package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.FormVersion

class AssessmentCreatedEvent(
  val formVersion: FormVersion,
  val properties: Map<String, List<String>>,
) : Event
