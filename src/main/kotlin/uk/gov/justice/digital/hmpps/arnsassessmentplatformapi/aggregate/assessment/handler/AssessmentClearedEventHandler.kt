package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentClearedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class AssessmentClearedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentClearedEvent> {
  override val eventType = AssessmentClearedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentClearedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    state.getForWrite(clock).data.apply {
      answers.clear()
      event.data.properties.forEach { properties.remove(it) }
      collections.filter { it.name in event.data.collections }.forEach { it.items.clear() }
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
        mapOf(
          "properties" to event.data.properties,
          "collections" to event.data.collections,
        ),
      ),
    )
  }
}
