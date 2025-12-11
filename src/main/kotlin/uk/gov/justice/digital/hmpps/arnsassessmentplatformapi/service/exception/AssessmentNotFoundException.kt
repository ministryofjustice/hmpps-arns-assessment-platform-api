package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier

class AssessmentNotFoundException(
  assessmentIdentifier: AssessmentIdentifier,
) : AssessmentPlatformException(
  message = "Assessment not found",
  developerMessage = "No assessment found for identifier: $assessmentIdentifier",
  statusCode = HttpStatus.NOT_FOUND,
)
