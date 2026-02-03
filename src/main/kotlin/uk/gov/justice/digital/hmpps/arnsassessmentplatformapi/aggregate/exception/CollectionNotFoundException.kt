package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionNotFoundException(collectionUuid: UUID, aggregateUuid: UUID? = null) :
  AssessmentPlatformException(
    message = "Collection not found",
    developerMessage = if (aggregateUuid !== null) {
      "No collection found with the UUID \"${collectionUuid}\" on aggregate \"$aggregateUuid\""
    } else {
      "No collection found with the UUID \"${collectionUuid}\""
    },
    statusCode = HttpStatus.BAD_REQUEST,
  )
