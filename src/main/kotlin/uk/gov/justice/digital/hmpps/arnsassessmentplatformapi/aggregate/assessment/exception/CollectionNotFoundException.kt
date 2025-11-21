package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionNotFoundException(collectionUuid: UUID) :
  AssessmentPlatformException(
    message = "Collection not found",
    developerMessage = "No collection found with the UUID \"${collectionUuid}\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
