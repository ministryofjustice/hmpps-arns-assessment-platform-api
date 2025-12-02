package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class GroupCommand(
  override val user: User,
  override val assessmentUuid: UUID,
  val commands: List<RequestableCommand>,
  override val timeline: Timeline? = null,
) : RequestableCommand
