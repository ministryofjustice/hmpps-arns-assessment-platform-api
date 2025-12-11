package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier

class DuplicateExternalIdentifierException(identifier: ExternalIdentifier) :
  AssessmentPlatformException(
    message = "The provided identifier already exists",
    developerMessage = "Duplicate identifier: $identifier",
    statusCode = HttpStatus.BAD_REQUEST,
  )
