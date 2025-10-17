package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class QueryHandlerNotImplementedException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Unable to dispatch query",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )
