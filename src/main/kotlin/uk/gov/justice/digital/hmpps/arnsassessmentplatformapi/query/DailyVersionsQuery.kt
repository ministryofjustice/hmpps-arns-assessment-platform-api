package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class DailyVersionsQuery(
  override val user: UserDetails,
  override val assessmentIdentifier: AssessmentIdentifier,
  override val timestamp: LocalDateTime? = null,
) : AssessmentQuery
