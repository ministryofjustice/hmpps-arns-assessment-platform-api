package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class UpdateFormVersionCommand(
  override val user: User,
  override val assessmentUuid: UUID,
  val version: String,
) : RequestableCommand
