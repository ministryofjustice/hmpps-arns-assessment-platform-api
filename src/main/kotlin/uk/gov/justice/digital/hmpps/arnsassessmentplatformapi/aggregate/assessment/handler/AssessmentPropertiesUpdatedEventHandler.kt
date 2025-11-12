package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

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
      event.data.timeline?.run(timeline::add)
    }

    state.get().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  private fun updateProperties(state: AssessmentState, event: AssessmentPropertiesUpdatedEvent) {
    with(state.get().data) {
      event.added.entries.map {
        properties.put(it.key, it.value)
        deletedProperties.remove(it.key)
      }
      event.removed.map { fieldCode ->
        properties[fieldCode]?.let { value ->
          properties.remove(fieldCode)
          deletedProperties.put(
            fieldCode,
            value,
          )
        }
      }
    }
  }
}
