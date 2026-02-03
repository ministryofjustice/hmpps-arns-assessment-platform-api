package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class UserDetailsNotFoundException :
  AssessmentPlatformException(
    message = "User not found",
    developerMessage = "User not found",
    statusCode = HttpStatus.NOT_FOUND,
  )
