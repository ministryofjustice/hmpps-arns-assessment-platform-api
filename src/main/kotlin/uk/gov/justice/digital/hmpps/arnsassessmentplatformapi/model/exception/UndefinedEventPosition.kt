package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class UndefinedEventPosition(eventUuid: UUID) :
  AssessmentPlatformException(
    message = "Event with UUID: $eventUuid does not have a valid position",
    developerMessage = "Event with UUID: $eventUuid does not have a valid position",
    statusCode = HttpStatus.BAD_REQUEST,
  )
