package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem

data class FormVersionUpdatedEvent(
  val version: String,
  override val timeline: TimelineItem?,
) : Event
