package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class GroupCommand(
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  val commands: List<RequestableCommand>,
  override val timeline: Timeline? = null,
) : RequestableCommand {
  override lateinit var receivedOn: LocalDateTime
}
