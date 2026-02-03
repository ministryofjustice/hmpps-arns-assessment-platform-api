package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionItemNotFoundException(collectionItemUuid: UUID, aggregateUuid: UUID? = null) :
  AssessmentPlatformException(
    message = "Collection item not found",
    developerMessage = if (aggregateUuid !== null) {
      "No collection item found with the UUID \"${collectionItemUuid}\" on aggregate \"$aggregateUuid\""
    } else {
      "No collection item found with the UUID \"${collectionItemUuid}\""
    },
    statusCode = HttpStatus.BAD_REQUEST,
  )
