package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

class CollectionNotFoundException(
  collectionUuid: UUID,
) : AssessmentPlatformException(
  message = "Assessment not found",
  developerMessage = "No assessment found with UUID: $collectionUuid",
  statusCode = HttpStatus.NOT_FOUND,
)
