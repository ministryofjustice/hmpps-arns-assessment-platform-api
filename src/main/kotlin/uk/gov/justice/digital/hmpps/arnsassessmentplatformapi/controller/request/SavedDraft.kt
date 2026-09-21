package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class SavedDraft(
  val assessmentUuid: UUID,
  val user: UserDetails,
  val draftKey: String,
  val formVersion: String,
  val revision: Long,
)
