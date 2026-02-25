package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.getUuidAt
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import java.util.UUID

data class Reference(
  private val rawValue: String,
) {
  lateinit var value: UUID

  fun resolve(context: List<CommandResponse>) {
    value = if (rawValue.startsWith("@")) {
      context.getUuidAt(rawValue.drop(1).toInt())
    } else {
      UUID.fromString(rawValue)
    }
  }

  fun resolve() = resolve(emptyList())

  companion object {
    @JvmStatic
    @JsonCreator
    fun from(value: String) = Reference(value)
  }
}

fun UUID.toReference() = Reference(this.toString()).also { it.resolve() }
