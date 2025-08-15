package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("ANSWERS_ROLLED_BACK")
data class AnswersRolledBack(
  val added: Map<String, List<String>>,
) : Event
