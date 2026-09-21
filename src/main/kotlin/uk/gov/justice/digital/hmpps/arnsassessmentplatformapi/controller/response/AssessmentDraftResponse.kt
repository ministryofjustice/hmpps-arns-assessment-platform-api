package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response

import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

data class AssessmentDraftResponse(
  val assessmentUuid: UUID,
  val draftKey: String,
  val formVersion: String,
  val baseAggregateUuid: UUID,
  val baseAggregateVersion: Long,
  val revision: Long,
  val data: JsonNode,
  val updatedAt: LocalDateTime,
  val expiresAt: LocalDateTime,
)
