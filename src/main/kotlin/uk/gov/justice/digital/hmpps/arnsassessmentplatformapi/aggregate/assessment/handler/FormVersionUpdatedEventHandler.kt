package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class FormVersionUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<FormVersionUpdatedEvent> {
  override val eventType = FormVersionUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<FormVersionUpdatedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    val aggregate = state.getForWrite(clock)

    aggregate.data.apply {
      formVersion = event.data.version
      collaborators.add(event.user.uuid)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return EventHandlerResult(
      state = state,
      timeline = TimelineEntity.resolver(
        event,
        mapOf(
          "version" to event.data.version,
        ),
      ),
    )
  }
}
