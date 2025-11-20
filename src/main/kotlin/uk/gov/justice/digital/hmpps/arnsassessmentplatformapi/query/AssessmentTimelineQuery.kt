package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import java.time.LocalDateTime
import java.util.UUID

data class AssessmentTimelineQuery(
  override val user: User,
  override val assessmentUuid: UUID,
  override val timestamp: LocalDateTime? = null,
  val timelineTypes: List<String>? = null,
) : RequestableQuery
