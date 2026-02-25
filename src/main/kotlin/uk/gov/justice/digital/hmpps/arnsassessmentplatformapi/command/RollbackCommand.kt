package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class RollbackCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val pointInTime: LocalDateTime,
  override val timeline: Timeline? = null,
) : RequestableCommand
