package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface CommandResult {
  val success: Boolean
  val message: String
}
