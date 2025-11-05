package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentAnswersUpdatedEventHandler(
  private val clock: Clock,
): EventHandler<AssessmentAnswersUpdatedEvent, AssessmentState> {

  override val eventType = AssessmentAnswersUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateAnswers(state, event.data)
    updateTimeline(state, event.data, event.createdAt)

    state.current().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  fun updateTimeline(state: AssessmentState, event: AssessmentAnswersUpdatedEvent, timestamp: LocalDateTime) {
    TimelineItem(
      timestamp = timestamp,
      details = "${event.added.size} answers updated and ${event.removed.size} removed",
    ).run(state.current().data.timeline::add)
  }

  fun updateAnswers(state: AssessmentState, event: AssessmentAnswersUpdatedEvent) {
    with (state.current().data) {
      event.added.entries.map {
        answers.put(it.key, it.value)
        deletedAnswers.remove(it.key)
      }
      event.removed.map { fieldCode ->
        answers[fieldCode]?.let { value ->
          answers.remove(fieldCode)
          deletedAnswers.put(
            fieldCode,
            value,
          )
        }
      }
    }
  }
}
