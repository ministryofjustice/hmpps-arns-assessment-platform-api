package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class InvalidCommandException(
  message: String,
) : AssessmentPlatformException(
  message = "Unable to process commands",
  developerMessage = message,
  statusCode = HttpStatus.BAD_REQUEST,
)
