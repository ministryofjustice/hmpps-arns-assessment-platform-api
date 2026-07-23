package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import java.util.UUID

data class EventHandlingException(
  val eventUuid: UUID,
  val eventName: String,
  val handlerName: String,
  override val cause: Throwable,
) : AssessmentPlatformException(
  message = "$handlerName was unable to handle $eventName with UUID: $eventUuid",
  developerMessage = cause.message ?: "",
  statusCode = HttpStatus.BAD_REQUEST,
)
