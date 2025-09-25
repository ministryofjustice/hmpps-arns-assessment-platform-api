package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class InvalidCommandException(
  message: String,
) : HttpClientErrorException(
  HttpStatus.BAD_REQUEST,
  message,
)
