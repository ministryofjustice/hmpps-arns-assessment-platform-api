package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentPropertiesUpdatedEventHandler(
  private val clock: Clock,
  stateService: StateService,
) : AssessmentEventHandler<AssessmentPropertiesUpdatedEvent>(stateService) {

  override val eventType = AssessmentPropertiesUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentPropertiesUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateTimeline(state, event.data, event.createdAt)
    updateProperties(state, event.data)

    state.get().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  private fun updateTimeline(state: AssessmentState, event: AssessmentPropertiesUpdatedEvent, timestamp: LocalDateTime) {
    event.added.entries.map {
      val previous = state.get().data.properties[it.key]
      when {
        previous.isNullOrEmpty() -> "Assessment property '${it.key}' set to '${it.value}'"
        previous == it.value -> null
        else -> "Assessment property '${it.key}' changed from '$previous' to '${it.value}'"
      }?.let { details ->
        TimelineItem(
          timestamp = timestamp,
          details = details,
        ).run(state.get().data.timeline::add)
      }
    }
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
