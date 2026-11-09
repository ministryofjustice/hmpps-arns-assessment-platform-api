package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class AssessmentNotStrengthsAndNeedsException(assessmentUuid: UUID) :
  AssessmentPlatformException(
    message = "Assessment is not a Strengths and Needs assessment",
    developerMessage = "Assessment with UUID: $assessmentUuid is not a Strengths and Needs assessment",
    statusCode = HttpStatus.BAD_REQUEST,
  )
