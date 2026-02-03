package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class GroupCommand(
  override val user: UserDetails,
  override val assessmentUuid: UUID,
  val commands: List<RequestableCommand>,
  override val timeline: Timeline? = null,
) : RequestableCommand
