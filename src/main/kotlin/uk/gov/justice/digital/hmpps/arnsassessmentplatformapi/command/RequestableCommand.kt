package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

sealed interface RequestableCommand : Command {
  val user: User
  val assessmentUuid: UUID
}
