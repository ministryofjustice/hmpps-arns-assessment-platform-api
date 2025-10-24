package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class UpdateAssessmentStatusCommand(
  override val user: User,
  override val collectionUuid: UUID,
  val status: String,
) : RequestableCommand
