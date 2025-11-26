package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class AggregateTypeNotFoundException(aggregateType: String) :
  AssessmentPlatformException(
    message = "Property not found",
    developerMessage = "The aggregate registered for the type \"${aggregateType}\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
