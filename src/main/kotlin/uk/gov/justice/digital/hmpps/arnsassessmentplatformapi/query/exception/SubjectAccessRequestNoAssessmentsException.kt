package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierPair

class SubjectAccessRequestNoAssessmentsException(identifiers: Collection<IdentifierPair>) :
  AssessmentPlatformException(
    message = "There are no assessments",
    developerMessage = "There were no assessments found for the identifiers: ${
      identifiers.joinToString(", ") { "${it.type}=${it.id}" }
    }",
    statusCode = HttpStatus.NO_CONTENT,
  )
