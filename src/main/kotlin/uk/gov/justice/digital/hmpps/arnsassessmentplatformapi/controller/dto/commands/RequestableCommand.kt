package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.util.UUID

sealed interface RequestableCommand : Command {
  val user: User
  val assessmentUuid: UUID
}
