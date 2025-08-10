package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.Command
import java.util.UUID

data class CommandRequest(
  val user: User,
  val assessmentUuid: UUID,
  val commands: List<Command>,
)
