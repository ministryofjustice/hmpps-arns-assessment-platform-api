package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class TimelineNotFoundException(uuid: UUID) :
  AssessmentPlatformException(
    message = "Timeline not found",
    developerMessage = "Timeline $uuid not found",
    statusCode = HttpStatus.NOT_FOUND,
  )
