package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("UPDATE_ANSWERS")
class UpdateAnswers(
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Command
