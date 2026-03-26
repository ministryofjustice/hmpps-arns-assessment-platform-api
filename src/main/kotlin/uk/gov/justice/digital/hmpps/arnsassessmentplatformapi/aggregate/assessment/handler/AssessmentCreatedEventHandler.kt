package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventProto
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class AssessmentCreatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentCreatedEvent> {
  override val eventType = AssessmentCreatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventProto<AssessmentCreatedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    updateProperties(state, event.data)
    state.getForWrite(clock).data.apply {
      formVersion = event.data.formVersion
      collaborators.add(event.user.uuid)
      flags.addAll(event.data.flags)
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
        mapOf(
          "formVersion" to event.data.formVersion,
          "properties" to event.data.properties.keys,
        ),
      ),
    )
  }

  private fun updateProperties(state: AssessmentState, event: AssessmentCreatedEvent) {
    state.getForWrite(clock).data.properties.putAll(event.properties)
  }
}
