package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class AssignedToUserEvent(
  val userUuid: UUID,
) : Event
