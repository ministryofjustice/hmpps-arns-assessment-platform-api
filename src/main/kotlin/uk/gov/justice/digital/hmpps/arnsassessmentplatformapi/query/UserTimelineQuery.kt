package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class UserTimelineQuery(
  override val user: UserDetails,
  override val timestamp: LocalDateTime?,
  val subject: UserDetails,
  val window: Window,
) : RequestableQuery
