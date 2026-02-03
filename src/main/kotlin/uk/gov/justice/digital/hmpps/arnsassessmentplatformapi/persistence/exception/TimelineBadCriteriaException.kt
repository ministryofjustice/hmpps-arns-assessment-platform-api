package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class TimelineBadCriteriaException :
  AssessmentPlatformException(
    message = "Unable to execute timeline query",
    developerMessage = "Must specify at least one of assessmentUuid or userUuid",
    statusCode = HttpStatus.BAD_REQUEST,
  )
