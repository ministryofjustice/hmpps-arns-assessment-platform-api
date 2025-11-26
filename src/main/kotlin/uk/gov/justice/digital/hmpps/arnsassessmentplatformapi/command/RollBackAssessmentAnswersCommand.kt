package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.time.LocalDateTime
import java.util.UUID

data class RollBackAssessmentAnswersCommand(
  override val user: User,
  override val assessmentUuid: UUID,
  val pointInTime: LocalDateTime,
  override val timeline: Timeline? = null,
) : RequestableCommand
