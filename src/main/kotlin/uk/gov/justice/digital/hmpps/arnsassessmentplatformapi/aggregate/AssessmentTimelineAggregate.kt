package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import kotlin.reflect.KClass

data class TimelineItem(
  val details: String = "",
  val timestamp: LocalDateTime,
)

class AssessmentTimelineAggregate : Aggregate {
  private val timeline = mutableListOf<TimelineItem>()
  private var previousStatus: String? = null

  private fun handle(timestamp: LocalDateTime, event: AssessmentAnswersUpdatedEvent) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "${event.added.size} answers updated and ${event.removed.size} removed",
      ),
    )
    numberOfEventsApplied += 1
  }

  private fun handle(timestamp: LocalDateTime, event: AssessmentAnswersRolledBackEvent) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "Rolled back ${event.added.size + event.removed.size} answers",
      ),
    )
    numberOfEventsApplied += 1
  }

  private fun handle(timestamp: LocalDateTime, event: AssessmentStatusUpdatedEvent) {
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

  override fun apply(event: EventEntity): Boolean {
    when (event.data) {
      is AssessmentAnswersUpdatedEvent -> handle(event.createdAt, event.data)
      is AssessmentAnswersRolledBackEvent -> handle(event.createdAt, event.data)
      is AssessmentStatusUpdatedEvent -> handle(event.createdAt, event.data)
      else -> return false
    }

    return true
  }

  override fun shouldCreate(event: KClass<out Event>) = createsOn.contains(event) || (numberOfEventsApplied > 1 && numberOfEventsApplied % 50L == 0L)

  override fun shouldUpdate(event: KClass<out Event>) = updatesOn.contains(event)

  override fun clone() = AssessmentTimelineAggregate()
    .also {
      it.timeline.addAll(timeline)
      it.previousStatus = previousStatus
    }

  companion object : AggregateType {
    override val createsOn: Set<KClass<out Event>> = setOf(AssessmentCreatedEvent::class)
    override val updatesOn: Set<KClass<out Event>> = setOf(AssessmentAnswersUpdatedEvent::class, AssessmentAnswersRolledBackEvent::class, AssessmentStatusUpdatedEvent::class)
  }
}
