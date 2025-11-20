package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
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
    state.get().data.apply {
      collaborators.add(event.user)
      event.data.timeline?.let { timeline.add(it.item(event)) }
    }

    state.get().apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }

  private fun updateProperties(state: AssessmentState, event: AssessmentPropertiesUpdatedEvent) {
    with(state.get().data) {
      event.added.entries.forEach {
        properties.put(it.key, it.value)
        deletedProperties.remove(it.key)
      }
      event.removed.forEach { fieldCode ->
        val removedValue = properties[fieldCode]
        if (removedValue != null) {
          properties.remove(fieldCode)
          deletedProperties.put(
            fieldCode,
            removedValue,
          )
        } else {
          throw Error("Property not found")
        }
      }
    }
  }
}
