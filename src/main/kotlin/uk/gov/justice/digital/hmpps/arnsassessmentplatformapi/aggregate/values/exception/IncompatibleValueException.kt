package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

class IncompatibleValueException(original: Value, revised: Value) :
  AssessmentPlatformException(
    message = "Cannot diff incompatible types of values",
    developerMessage = "Original value type ${original::class} cannot be diffed with revised value type ${revised::class}",
    statusCode = HttpStatus.BAD_REQUEST,
  )
