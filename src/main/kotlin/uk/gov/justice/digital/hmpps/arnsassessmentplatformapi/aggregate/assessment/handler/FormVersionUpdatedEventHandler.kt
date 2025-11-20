package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class FormVersionUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<FormVersionUpdatedEvent> {
  override val eventType = FormVersionUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<FormVersionUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    aggregate.data.apply {
      formVersion = event.data.version
      collaborators.add(event.user)
      event.data.timeline?.let { timeline.add(it.item(event)) }
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
