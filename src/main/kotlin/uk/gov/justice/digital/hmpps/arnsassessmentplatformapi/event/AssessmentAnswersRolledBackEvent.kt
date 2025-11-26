package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.time.LocalDateTime

data class AssessmentAnswersRolledBackEvent(
  val rolledBackTo: LocalDateTime,
  override val timeline: Timeline?,
) : Event
