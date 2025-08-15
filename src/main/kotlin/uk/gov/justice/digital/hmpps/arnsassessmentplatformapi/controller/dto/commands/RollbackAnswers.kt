package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import java.util.UUID

@JsonTypeName("UPDATE_ANSWERS")
class RollbackAnswers(
  val eventUuid: UUID,
) : Command {
  override fun toEvent() = AnswersRolledBack(emptyMap())
}
