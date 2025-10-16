package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class AssessmentNotFoundException(
  assessmentUuid: UUID,
) : AssessmentPlatformException(
  message = "Assessment not found",
  developerMessage = "No assessment found with UUID: $assessmentUuid",
  statusCode = HttpStatus.NOT_FOUND,
)
