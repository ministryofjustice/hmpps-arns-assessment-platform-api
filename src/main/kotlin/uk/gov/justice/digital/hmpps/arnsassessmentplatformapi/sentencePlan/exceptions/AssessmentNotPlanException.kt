package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan.exceptions

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class AssessmentNotPlanException(assessmentUuid: UUID) :
  AssessmentPlatformException(
    message = "Assessment is not a Sentence Plan",
    developerMessage = "Assessment with UUID: $assessmentUuid is not a Sentence Plan",
    statusCode = HttpStatus.BAD_REQUEST,
  )
