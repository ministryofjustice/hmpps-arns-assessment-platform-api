package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class PropertyNotFoundException(propertyName: String, aggregateUuid: UUID) :
  AssessmentPlatformException(
    message = "Property not found",
    developerMessage = "The property \"$propertyName\" could not be found on aggregate \"$aggregateUuid\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
