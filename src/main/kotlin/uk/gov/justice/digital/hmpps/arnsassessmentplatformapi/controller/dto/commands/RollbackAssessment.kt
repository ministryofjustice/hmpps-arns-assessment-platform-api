package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import java.time.LocalDateTime

class RollbackAssessment(
  val dateAndTime: LocalDateTime,
) : Command {
  override fun toEvent() = AnswersRolledBack(
    added = emptyMap(),
    removed = emptyList(),
  )
}
