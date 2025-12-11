package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.time.LocalDateTime

class InvalidTimestampException(timestamp: LocalDateTime, message: String) :
  AssessmentPlatformException(
    message = "Invalid timestamp $timestamp",
    developerMessage = message,
    statusCode = HttpStatus.BAD_REQUEST,
  )
