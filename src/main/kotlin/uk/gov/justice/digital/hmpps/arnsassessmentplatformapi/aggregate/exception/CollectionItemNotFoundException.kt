package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionItemNotFoundException(collectionItemUuid: UUID, aggregateUuid: UUID) :
  AssessmentPlatformException(
    message = "Collection item not found",
    developerMessage = "No collection item found with the UUID \"${collectionItemUuid}\" on aggregate \"$aggregateUuid\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
