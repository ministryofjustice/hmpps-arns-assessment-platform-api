package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class AssessmentFlagsUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentFlagsUpdatedEvent> {
  override val eventType = AssessmentFlagsUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentFlagsUpdatedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    state.getForWrite(clock).data.apply {
      flags.clear()
      flags.addAll(event.data.flags)
      collaborators.add(event.user.uuid)
    }

    state.getForWrite(clock).apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return EventHandlerResult(
      state = state,
      timeline = TimelineEntity.resolver(
        event,
        mapOf("flags" to event.data.flags),
      ),
    )
  }
}
