package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.time.LocalDateTime
import java.util.UUID

class UndeleteNotAtTailException(assessmentUuid: UUID, pointInTime: LocalDateTime) :
  AssessmentPlatformException(
    message = "Unable to undelete",
    developerMessage = "Events deleted from $pointInTime for assessment $assessmentUuid are not at the end of the event stream, so they cannot be safely undeleted",
    statusCode = HttpStatus.CONFLICT,
  )
