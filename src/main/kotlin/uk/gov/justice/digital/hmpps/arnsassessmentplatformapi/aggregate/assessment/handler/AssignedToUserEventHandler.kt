package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class AssignedToUserEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssignedToUserEvent> {
  override val eventType = AssignedToUserEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssignedToUserEvent>,
    state: AssessmentState,
  ): AssessmentState {
    state.getForWrite().data.apply {
      assignedUser = event.data.userUuid
    }

    state.getForWrite().apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
