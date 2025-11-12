package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem

data class AssessmentAnswersUpdatedEvent(
  val added: Map<String, List<String>>,
  val removed: List<String>,
  override val timeline: TimelineItem?,
) : Event
