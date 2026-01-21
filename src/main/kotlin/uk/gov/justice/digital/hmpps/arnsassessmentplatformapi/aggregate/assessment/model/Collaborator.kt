package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.util.UUID

data class Collaborator(
  val uuid: UUID,
  val displayName: String,
) {
  companion object {
    fun from(user: UserDetailsEntity) = Collaborator(user.uuid, user.displayName)
  }
}
