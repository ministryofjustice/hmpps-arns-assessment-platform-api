package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

data class UpdateAssessmentAnswersCommand(
  override val user: User,
  override val assessmentUuid: UUID,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : RequestableCommand
