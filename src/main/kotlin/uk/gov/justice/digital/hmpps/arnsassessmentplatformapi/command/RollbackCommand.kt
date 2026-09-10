package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import java.time.LocalDateTime

data class RollbackCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val pointInTime: LocalDateTime,
  override val timeline: Timeline? = null,
  override val hooks: List<Hook>? = null,
) : RequestableCommand
