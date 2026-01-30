package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

sealed interface RequestableCommand : Command {
  val user: UserDetails
  val assessmentUuid: UUID
}
