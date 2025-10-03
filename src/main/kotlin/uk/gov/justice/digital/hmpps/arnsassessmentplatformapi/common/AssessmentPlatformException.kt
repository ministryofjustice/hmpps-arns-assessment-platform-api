package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

abstract class AssessmentPlatformException(
  message: String,
  val developerMessage: String,
  val statusCode: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
) : RuntimeException(message) {
  fun intoResponse(): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(statusCode)
    .body(
      ErrorResponse(
        userMessage = message.orEmpty(),
        developerMessage = developerMessage,
        status = statusCode,
      ),
    )
}
