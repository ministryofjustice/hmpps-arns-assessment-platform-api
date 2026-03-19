package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import java.time.LocalDateTime

data class CreateTimelineItemCommand(
  val timestamp: LocalDateTime,
  override val user: UserDetails,
  override val assessmentUuid: Reference,
  override val timeline: Timeline,
) : RequestableCommand
