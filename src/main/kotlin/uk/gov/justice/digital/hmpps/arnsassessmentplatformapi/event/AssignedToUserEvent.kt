package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.util.UUID

data class AssignedToUserEvent(
  val userUuid: UUID,
  override val timeline: Timeline?,
) : Event
