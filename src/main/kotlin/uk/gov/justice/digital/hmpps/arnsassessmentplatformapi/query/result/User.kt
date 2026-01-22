package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.*

data class User(
  val id: UUID,
  val name: String,
) {
  companion object {
    fun from(entity: UserDetailsEntity?) = entity
      ?.run { User(uuid, displayName) }
      ?: User(UUID.fromString("00000000-0000-0000-0000-000000000000"), "UNKNOWN")
  }
}
