package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentStatusUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.Event
import java.time.LocalDateTime

private const val TYPE = "ASSESSMENT_TIMELINE"

data class TimelineItem(
  val details: String = "",
  val timestamp: LocalDateTime,
)

@JsonTypeName(TYPE)
class AssessmentTimelineAggregate : Aggregate {
  private val timeline = mutableListOf<TimelineItem>()
  private var previousStatus = ""

  fun handle(timestamp: LocalDateTime, event: AnswersUpdated) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "${event.added.size} answers updated and ${event.removed.size} removed",
      ),
    )
    numberOfEventsApplied += 1
  }

  fun handle(timestamp: LocalDateTime, event: AnswersRolledBack) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "Rolled back ${event.added.size + event.removed.size} answers",
      ),
    )
    numberOfEventsApplied += 1
  }

  fun handle(timestamp: LocalDateTime, event: AssessmentStatusUpdated) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "Assessment status changed from \"$previousStatus\" to \"${event.status}\"",
      ),
    )
    numberOfEventsApplied += 1
    previousStatus = event.status
  }

  fun getTimeline() = this.timeline.toList()

  override var numberOfEventsApplied: Long = 0

  override fun apply(event: EventEntity): Boolean {
    when (event.data) {
      is AnswersUpdated -> handle(event.createdAt, event.data)
      is AnswersRolledBack -> handle(event.createdAt, event.data)
      is AssessmentStatusUpdated -> handle(event.createdAt, event.data)
      else -> return false
    }

    return true
  }

  override fun shouldCreate(event: Event) = createsOn.contains(event::class) || numberOfEventsApplied % 50L == 0L

  override fun shouldUpdate(event: Event) = updatesOn.contains(event::class)

  override fun clone() = AssessmentTimelineAggregate()
    .also {
      it.timeline.plus(timeline)
      it.previousStatus = previousStatus
    }

  companion object : AggregateType {
    override val getInstance = { AssessmentTimelineAggregate() }
    override val aggregateType = TYPE
    override val createsOn = setOf(AssessmentCreated::class)
    override val updatesOn = setOf(AnswersUpdated::class, AnswersRolledBack::class, AssessmentStatusUpdated::class)
  }
}
