package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class UndefinedPosition(message: String) :
  AssessmentPlatformException(
    message = message,
    developerMessage = message,
    statusCode = HttpStatus.BAD_REQUEST,
  )
