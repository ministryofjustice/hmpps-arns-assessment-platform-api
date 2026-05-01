package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

class GetAssessmentsSoftDeletedSinceQuery(
  val assessmentType: String,
  val since: LocalDateTime,
  override val user: UserDetails,
  override val timestamp: LocalDateTime? = null
) : RequestableQuery