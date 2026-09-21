package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails

data class AssessmentDraftQueryRequest(
  val user: UserDetails,
)
