package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class AggregateNotRegisteredException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Aggregate not registered",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )
