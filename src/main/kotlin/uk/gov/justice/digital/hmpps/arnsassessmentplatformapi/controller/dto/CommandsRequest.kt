package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.RequestableCommand

class InvalidCommandException(
  message: String,
) : AssessmentPlatformException(
  message = "Unable to process commands",
  developerMessage = message,
  statusCode = HttpStatus.BAD_REQUEST,
)

data class CommandsRequest(
  val commands: List<RequestableCommand>,
) {
  init {
    if (commands.isEmpty()) throw InvalidCommandException("No commands received")
  }
}
