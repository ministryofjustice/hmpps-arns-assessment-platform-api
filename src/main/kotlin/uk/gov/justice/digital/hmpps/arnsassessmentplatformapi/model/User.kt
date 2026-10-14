package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.UUID

data class User(
  val id: UUID,
  val name: String,
  val userId: String? = null,
  val authSource: AuthSource? = null,
) {
  companion object {
    fun from(entity: UserDetailsEntity?): User = entity
      ?.run {
        User(
          id = this.uuid,
          name = this.displayName,
          userId = this.userId,
          authSource = this.authSource,
        )
      }
      ?: User(
        id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
        name = "UNKNOWN",
      )
  }
}
