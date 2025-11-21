package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionItemNotFoundException(collectionItemUuid: UUID) :
  AssessmentPlatformException(
    message = "Collection item not found",
    developerMessage = "No collection item found with the UUID \"${collectionItemUuid}\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
