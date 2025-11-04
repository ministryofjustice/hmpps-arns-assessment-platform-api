package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class EventHandlerNotImplementedException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Unable to handle event",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )
