package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class TimelineQuery(
  override val user: UserDetails,
  override val timestamp: LocalDateTime? = null,
  override val pageNumber: Int = 0,
  override val pageSize: Int = 50,
  val assessmentIdentifier: AssessmentIdentifier? = null,
  val subject: UserDetails? = null,
  val from: LocalDateTime? = null,
  val to: LocalDateTime? = null,
  val includeEventTypes: Set<String>? = null,
  val excludeEventTypes: Set<String>? = null,
  val includeCustomTypes: Set<String>? = null,
  val excludeCustomTypes: Set<String>? = null,
) : RequestableQuery,
  PageableQuery
