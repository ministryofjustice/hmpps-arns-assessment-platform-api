package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentCreatedEventHandler(
  private val clock: Clock,
): EventHandler<AssessmentCreatedEvent, AssessmentState> {
  override val eventType = AssessmentCreatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentCreatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateTimeline(state, event.data, event.createdAt)
    updateProperties(state, event.data)

    state.current().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  fun updateTimeline(state: AssessmentState, event: AssessmentCreatedEvent, timestamp: LocalDateTime) {
    TimelineItem(
      timestamp = timestamp,
      details = "Assessment created with ${event.properties.size} properties",
    ).run(state.current().data.timeline::add)
  }

  fun updateProperties(state: AssessmentState, event: AssessmentCreatedEvent) {
    state.current().data.properties.putAll(event.properties)
  }
}
