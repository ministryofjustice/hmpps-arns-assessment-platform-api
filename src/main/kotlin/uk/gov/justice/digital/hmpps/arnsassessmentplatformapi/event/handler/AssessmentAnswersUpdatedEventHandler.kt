package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Service
class AssessmentAnswersUpdatedEventHandler(
  private val clock: Clock,
): EventHandler<AssessmentAnswersUpdatedEvent> {
  override val type = AssessmentAnswersUpdatedEvent::class
  override fun handle(
    event: EventEntity,
    data: AssessmentAnswersUpdatedEvent,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.current()

    updateAnswers(state, data)
    updateTimeline(state, data)

    aggregate.eventsTo = event.createdAt
    aggregate.updatedAt = LocalDateTime.now(clock)
    aggregate.numberOfEventsApplied++

    return state
  }

  private fun updateTimeline(state: AssessmentState, event: AssessmentAnswersUpdatedEvent) {
    state.current().data.timeline.add("Added ${event.added.size} answers")
  }

  private fun updateAnswers(state: AssessmentState, event: AssessmentAnswersUpdatedEvent) {
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
