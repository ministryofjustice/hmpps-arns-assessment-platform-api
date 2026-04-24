package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime
import java.util.UUID

data class GetAssessmentsModifiedSinceQuery(
  override val user: UserDetails,
  val assessmentType: String,
  val since: LocalDateTime,
  val after: UUID? = null,
  val limit: Int = 50,
  override val timestamp: LocalDateTime? = null,
) : RequestableQuery
