package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("ANSWERS_UPDATED")
data class AnswersUpdated(
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event
