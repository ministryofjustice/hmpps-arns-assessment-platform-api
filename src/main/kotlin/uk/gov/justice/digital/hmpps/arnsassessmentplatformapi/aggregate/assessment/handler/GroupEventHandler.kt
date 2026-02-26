package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class GroupEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<GroupEvent> {
  override val eventType = GroupEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<GroupEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.getForWrite(clock)

    aggregate.data.apply {
      collaborators.add(event.user.uuid)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
