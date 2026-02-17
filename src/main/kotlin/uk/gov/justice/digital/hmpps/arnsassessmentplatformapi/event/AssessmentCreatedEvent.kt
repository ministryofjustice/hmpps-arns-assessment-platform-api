package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class AssessmentCreatedEvent(
  val formVersion: FormVersion,
  val properties: Map<String, Value>,
  val flags: List<String> = emptyList(),
) : Event
