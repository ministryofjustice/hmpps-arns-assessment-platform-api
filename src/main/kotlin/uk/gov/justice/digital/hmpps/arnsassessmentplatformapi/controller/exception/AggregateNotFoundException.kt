package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class AggregateNotFoundException(
  message: String,
) : HttpClientErrorException(
  HttpStatus.NOT_FOUND,
  message,
)
