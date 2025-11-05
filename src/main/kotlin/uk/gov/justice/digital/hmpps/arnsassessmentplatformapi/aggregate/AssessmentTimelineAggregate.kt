package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import java.time.LocalDateTime

class AssessmentTimelineAggregate : Aggregate {
  private val timeline = mutableListOf<TimelineItem>()
  private var previousStatus: String? = null

  private fun handle(timestamp: LocalDateTime, event: AssessmentAnswersRolledBackEvent) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "Rolled back ${event.added.size + event.removed.size} answers",
      ),
    )
    numberOfEventsApplied += 1
  }

  private fun handle(timestamp: LocalDateTime, event: AssessmentPropertiesUpdatedEvent) {
    val details = if (!previousStatus.isNullOrBlank()) {
      "Assessment status changed from \"$previousStatus\" to \"${event.status}\""
    } else {
      "Assessment status changed to \"${event.status}\""
    }

    timeline.add(TimelineItem(timestamp = timestamp, details = details))
    numberOfEventsApplied += 1
    previousStatus = event.status
  }

  fun getTimeline() = this.timeline.toList()

  override var numberOfEventsApplied: Long = 0
}
