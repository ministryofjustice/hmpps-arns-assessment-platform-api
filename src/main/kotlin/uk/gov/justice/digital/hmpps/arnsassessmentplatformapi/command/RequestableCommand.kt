package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

sealed interface RequestableCommand : Command {
  val user: UserDetails
  val assessmentUuid: Reference
}

sealed interface Reference {
  data class Uuid(val value: UUID) : Reference
  data class Placeholder(val index: Int) : Reference

  companion object {
    @JvmStatic
    @JsonCreator
    fun from(value: String): Reference =
      if (value.startsWith("@")) {
        Placeholder(value.drop(1).toInt())
      } else {
        Uuid(UUID.fromString(value))
      }
  }

}
