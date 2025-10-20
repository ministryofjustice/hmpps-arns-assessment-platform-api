package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.time.LocalDateTime

data class AnswersRolledBackEvent(
  val rolledBackTo: LocalDateTime,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event
