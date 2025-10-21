package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException

data class CommandsRequest(
  val commands: List<RequestableCommand>,
) {
  init {
    if (commands.isEmpty()) throw InvalidCommandException("No commands received")
  }
}
