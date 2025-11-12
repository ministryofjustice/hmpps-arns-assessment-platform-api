package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import java.time.LocalDateTime

data class AssessmentAnswersRolledBackEvent(
  val rolledBackTo: LocalDateTime,
  override val timeline: TimelineItem?,
) : Event
