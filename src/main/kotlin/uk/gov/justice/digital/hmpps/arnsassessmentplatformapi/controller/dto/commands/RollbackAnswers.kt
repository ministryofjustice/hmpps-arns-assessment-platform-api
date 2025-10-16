package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeName("ROLLBACK_ANSWERS")
data class RollbackAnswers(
  override val user: User,
  override val assessmentUuid: UUID,
  val pointInTime: LocalDateTime,
) : RequestableCommand
