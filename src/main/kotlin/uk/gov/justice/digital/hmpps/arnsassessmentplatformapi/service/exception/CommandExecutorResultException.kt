package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class CommandExecutorResultException(
  message: String,
  statusCode: HttpStatus? = null,
) : HttpClientErrorException(
  statusCode ?: HttpStatus.CONFLICT,
  message,
)
