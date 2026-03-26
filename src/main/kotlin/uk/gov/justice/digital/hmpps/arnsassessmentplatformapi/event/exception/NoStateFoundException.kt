package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class NoStateFoundException(developerMessage: String) :
  AssessmentPlatformException(
    message = "No state found.",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )
