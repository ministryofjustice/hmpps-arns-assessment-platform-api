package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import tools.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime
import java.util.UUID

data class AssessmentDraftRequest(
  val user: UserDetails,
  val draftKey: String,
  val formVersion: String,
  val baseAggregateUuid: UUID,
  val baseAggregateVersion: Long,
  val revision: Long,
  val data: JsonNode,
  val expiresAt: LocalDateTime,
)
