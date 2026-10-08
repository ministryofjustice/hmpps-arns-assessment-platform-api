package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity

data class UserDetails(
  val id: String,
  val name: String,
  val authSource: AuthSource = AuthSource.NOT_SPECIFIED,
) {
  companion object {
    fun from(entity: UserDetailsEntity) = UserDetails(
      id = entity.userId,
      name = entity.displayName,
      authSource = entity.authSource,
    )
  }
}
