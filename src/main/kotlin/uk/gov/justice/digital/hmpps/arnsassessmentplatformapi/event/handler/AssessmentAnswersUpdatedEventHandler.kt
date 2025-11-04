package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

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

    with (aggregate.data) {
      data.added.entries.map {
        answers.put(it.key, it.value)
        deletedAnswers.remove(it.key)
      }
      data.removed.map { fieldCode ->
        answers[fieldCode]?.let { value ->
          answers.remove(fieldCode)
          deletedAnswers.put(
            fieldCode,
            value,
          )
        }
      }
    }

    aggregate.eventsTo = event.createdAt
    aggregate.updatedAt = LocalDateTime.now(clock)
    aggregate.numberOfEventsApplied++

    return state
  }
}
