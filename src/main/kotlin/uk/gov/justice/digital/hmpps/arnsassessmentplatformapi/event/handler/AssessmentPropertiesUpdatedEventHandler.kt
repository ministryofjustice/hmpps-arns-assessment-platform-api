package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentPropertiesUpdatedEventHandler(
  private val clock: Clock,
): EventHandler<AssessmentPropertiesUpdatedEvent, AssessmentState> {

  override val eventType = AssessmentPropertiesUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentPropertiesUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateTimeline(state, event.data, event.createdAt)
    updateProperties(state, event.data)

    state.current().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  fun updateTimeline(state: AssessmentState, event: AssessmentPropertiesUpdatedEvent, timestamp: LocalDateTime) {
    event.added.entries.map {
      val previous = state.current().data.properties[it.key]
      when {
        previous.isNullOrEmpty() -> "Assessment property '${it.key}' set to '${it.value}'"
        previous == it.value -> null
        else -> "Assessment property '${it.key}' changed from '${previous}' to '${it.value}'"
      }?.let { details ->
        TimelineItem(
          timestamp = timestamp,
          details = details,
        ).run(state.current().data.timeline::add)
      }
    }
  }

  fun updateProperties(state: AssessmentState, event: AssessmentPropertiesUpdatedEvent) {
    with (state.current().data) {
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
