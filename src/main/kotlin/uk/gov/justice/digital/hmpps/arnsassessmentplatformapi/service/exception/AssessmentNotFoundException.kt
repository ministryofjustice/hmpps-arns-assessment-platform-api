package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier

class AssessmentNotFoundException(
  assessmentIdentifier: AssessmentIdentifier,
) : AssessmentPlatformException(
  message = "Assessment not found",
  developerMessage = when (assessmentIdentifier) {
    is ExternalIdentifier -> "No assessment found for ${assessmentIdentifier.identifierType}: ${assessmentIdentifier.identifier} and type: ${assessmentIdentifier.assessmentType}"
    is UuidIdentifier -> "No assessment found for UUID: ${assessmentIdentifier.uuid}"
  },
  statusCode = HttpStatus.NOT_FOUND,
)
