package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class GetAssessmentsModifiedSinceQuery(
  override val user: UserDetails,
  val assessmentType: String,
  val since: LocalDateTime,
  override val pageNumber: Int = 0,
  override val pageSize: Int = 50,
  override val timestamp: LocalDateTime? = null,
) : RequestableQuery,
  PageableQuery
