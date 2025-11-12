package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.FormVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem

class AssessmentCreatedEvent(
  val formVersion: FormVersion,
  val properties: Map<String, List<String>>,
  override val timeline: TimelineItem?,
) : Event
