package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User

data class AssessmentVersionResponse(
  val formVersion: String?,
  val answers: Map<String, List<String>>,
  val collaborators: Set<User>,
) : AggregateResponse
