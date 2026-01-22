package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.PropertyNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class AssessmentPropertiesUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentPropertiesUpdatedEvent> {

  override val eventType = AssessmentPropertiesUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentPropertiesUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateProperties(state, event.data)
    state.getForWrite().data.apply {
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

  private fun updateProperties(state: AssessmentState, event: AssessmentPropertiesUpdatedEvent) {
    with(state.getForWrite()) {
      event.added.entries.forEach {
        data.properties.put(it.key, it.value)
      }
      event.removed.forEach { propertyName ->
        val removedValue = data.properties[propertyName]
        if (removedValue != null) {
          data.properties.remove(propertyName)
        } else {
          throw PropertyNotFoundException(propertyName, uuid)
        }
      }
    }
  }
}
