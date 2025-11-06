package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.time.LocalDateTime

data class AssessmentAnswersRolledBackEvent(
  val rolledBackTo: LocalDateTime,
) : Event
