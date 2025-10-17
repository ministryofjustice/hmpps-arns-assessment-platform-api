package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class CommandHandlerNotImplementedException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Unable to dispatch command",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )
