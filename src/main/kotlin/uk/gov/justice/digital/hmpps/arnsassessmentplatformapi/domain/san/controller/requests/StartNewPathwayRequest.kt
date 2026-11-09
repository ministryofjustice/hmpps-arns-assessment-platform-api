package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.controller.requests

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class StartNewPathwayRequest(
  val user: UserDetails,
  val assessmentUuid: UUID,
  @Schema(description = "Pathway the existing answers were recorded under", example = "COMMUNITY")
  val previousPathway: String,
  @Schema(description = "Pathway the assessment is moving to", example = "CUSTODY")
  val newPathway: String,
)
