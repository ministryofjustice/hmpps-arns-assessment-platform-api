package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionDepthOutOfBoundsException(depth: Int, collectionUuid: UUID) :
  AssessmentPlatformException(
    message = "Collection depth out of bounds",
    developerMessage = "Invalid depth $depth for collection \"${collectionUuid}\"",
    statusCode = HttpStatus.BAD_REQUEST,
  )
