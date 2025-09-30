package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime

@JsonTypeName("ANSWERS_ROLLED_BACK")
data class AnswersRolledBack(
  val rolledBackTo: LocalDateTime,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event
