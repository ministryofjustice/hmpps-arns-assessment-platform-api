package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus.CONFLICT
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException

class DraftRevisionConflictException :
  AssessmentPlatformException(
    message = "A newer draft has already been saved",
    developerMessage = "Draft revision must increase monotonically",
    statusCode = CONFLICT,
  )
