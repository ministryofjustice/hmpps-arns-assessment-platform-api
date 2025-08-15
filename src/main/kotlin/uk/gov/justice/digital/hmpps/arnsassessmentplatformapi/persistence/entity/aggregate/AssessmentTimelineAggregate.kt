package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded
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
        details = "${event.added.size} questions added and ${event.removed.size} removed",
      ),
    )
  }

  fun handle(timestamp: LocalDateTime, event: OasysEventAdded) {
    timeline.add(
      TimelineItem(
        timestamp = timestamp,
        details = "OASys status changed from \"$previousStatus\" to \"${event.tag}\"",
      ),
    )
    previousStatus = event.tag
  }

  fun getTimeline() = this.timeline.toList()

  override fun applyAll(events: List<EventEntity>): AssessmentTimelineAggregate {
    events.sortedBy { it.createdAt }
      .forEach { event ->
        when (event.data) {
          is AnswersUpdated -> handle(event.createdAt, event.data)
          is OasysEventAdded -> handle(event.createdAt, event.data)
          else -> {}
        }
      }
    return this
  }

  companion object : AggregateType {
    override val getInstance = { AssessmentTimelineAggregate() }
    override val aggregateType = TYPE
    override val createsOn = setOf(AssessmentCreated::class)
    override val updatesOn = setOf(AnswersUpdated::class, OasysEventAdded::class)
  }
}
