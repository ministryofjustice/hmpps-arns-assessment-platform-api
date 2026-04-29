package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class EventNotFoundException(uuid: UUID) :
  AssessmentPlatformException(
    message = "Event not found",
    developerMessage = "Event $uuid not found",
    statusCode = HttpStatus.NOT_FOUND,
  )
