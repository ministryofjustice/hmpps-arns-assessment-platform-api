package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentCreatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentCreatedEvent> {
  override val eventType = AssessmentCreatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentCreatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateTimeline(state, event.data, event.createdAt)
    updateProperties(state, event.data)
    state.get().data.collaborators.add(event.user)

    state.get().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  private fun updateTimeline(state: AssessmentState, event: AssessmentCreatedEvent, timestamp: LocalDateTime) {
    TimelineItem(
      timestamp = timestamp,
      details = "Assessment created with ${event.properties.size} properties",
    ).run(state.get().data.timeline::add)
  }

  private fun updateProperties(state: AssessmentState, event: AssessmentCreatedEvent) {
    state.get().data.properties.putAll(event.properties)
  }
}
