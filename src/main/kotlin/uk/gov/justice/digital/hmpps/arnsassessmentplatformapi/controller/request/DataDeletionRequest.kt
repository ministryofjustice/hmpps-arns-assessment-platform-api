package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import java.util.UUID

data class DataDeletionRequest(
  val events: List<EventUpdate> = emptyList(),
  val timeline: List<TimelineUpdate> = emptyList(),
  val dryRun: Boolean = true,
)

data class EventUpdate(
  val uuid: UUID,
  val operation: DataDeletionOperation,
  val event: Event,
)

data class TimelineUpdate(
  val uuid: UUID,
  val operation: DataDeletionOperation,
  val timeline: TimelineItem,
)

enum class DataDeletionOperation {
  UPDATE,
  DELETE,
}
