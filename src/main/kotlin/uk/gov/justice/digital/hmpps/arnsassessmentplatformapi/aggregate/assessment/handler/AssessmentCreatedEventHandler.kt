package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

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
    updateProperties(state, event.data)
    state.getForWrite().data.apply {
      formVersion = event.data.formVersion
      collaborators.add(event.user.uuid)
      event.data.timeline?.let { timeline.add(it.item(event)) }
    }

    state.getForWrite().apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }

  private fun updateProperties(state: AssessmentState, event: AssessmentCreatedEvent) {
    state.getForWrite().data.properties.putAll(event.properties)
  }
}
