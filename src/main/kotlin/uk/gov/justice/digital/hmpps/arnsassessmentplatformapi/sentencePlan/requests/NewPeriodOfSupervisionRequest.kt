package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.sentencePlan.requests

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.util.UUID

data class NewPeriodOfSupervisionRequest(
  val user: UserDetails,
  val assessmentUuid: UUID,
)
