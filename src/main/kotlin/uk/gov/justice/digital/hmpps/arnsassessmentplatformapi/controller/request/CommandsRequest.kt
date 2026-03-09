package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException
import java.time.LocalDateTime

data class CommandsRequest(
  val commands: List<RequestableCommand>,
) {
  init {
    if (commands.isEmpty()) throw InvalidCommandException("No commands received")
  }
}

fun List<RequestableCommand>.addReceivedOn(timestamp: LocalDateTime): LocalDateTime {
  return fold(timestamp) { offsetTimestamp, command ->
    when (command) {
      is GroupCommand -> {
        command.commands.addReceivedOn(offsetTimestamp)
        command.apply { receivedOn = timestamp }.receivedOn
      }
      else -> command.apply { receivedOn = timestamp }.receivedOn
    }.plusNanos(100)
  }
}
