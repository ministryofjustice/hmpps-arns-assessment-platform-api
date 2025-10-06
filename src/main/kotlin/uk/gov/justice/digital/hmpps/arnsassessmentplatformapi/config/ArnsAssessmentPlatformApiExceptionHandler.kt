package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class ArnsAssessmentPlatformApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleException(e: ValidationException): ResponseEntity<ErrorResponse> {
    val statusCode = BAD_REQUEST
    return ResponseEntity
      .status(statusCode)
      .body(
        ErrorResponse(
          status = statusCode,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      ).also { logException(e, statusCode) }
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
    val statusCode = NOT_FOUND
    return ResponseEntity
      .status(e.statusCode.value())
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = "No resource found failure: ${e.message}",
          developerMessage = e.message,
        ),
      ).also { logException(e, statusCode) }
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    val statusCode = FORBIDDEN
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Forbidden: ${e.message}",
          developerMessage = e.message,
        ),
      ).also { logException(e, statusCode) }
  }

  @ExceptionHandler(AssessmentPlatformException::class)
  fun handleException(e: AssessmentPlatformException) = e.intoResponse().also { logException(e) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun logException(e: Exception, statusCode: HttpStatusCode? = INTERNAL_SERVER_ERROR) {
      when (e) {
        is AssessmentPlatformException -> log.debug("Status (${e.statusCode.value()}) returned: {}", e.developerMessage)
        else -> log.debug("Status ({}) returned: {}", statusCode, e.message)
      }
    }
  }
}
