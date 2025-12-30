package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class AssessmentCreatedEvent(
  val formVersion: FormVersion,
  val properties: Map<String, Value>,
  override val timeline: Timeline?,
) : Event
