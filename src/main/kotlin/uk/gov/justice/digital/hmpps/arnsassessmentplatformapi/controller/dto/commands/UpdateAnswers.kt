package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated

@JsonTypeName("UPDATE_ANSWERS")
class UpdateAnswers(
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Command {
  fun toEvent() = AnswersUpdated(
    added = added,
    removed = removed,
  )
}
