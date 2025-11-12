package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentAnswersUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentAnswersUpdatedEvent> {

  override val eventType = AssessmentAnswersUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersUpdatedEvent>,
    state: AssessmentState,
  ): AssessmentState {
    updateAnswers(state, event.data.added, event.data.removed)
    state.get().data.apply {
      collaborators.add(event.user)
      event.data.timeline?.run(timeline::add)
      updatedAt = event.createdAt
    }

    state.get().apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }

  companion object {
    fun updateAnswers(state: AssessmentState, added: Map<String, List<String>>, removed: List<String>) {
      with(state.get().data) {
        added.entries.map {
          answers.put(it.key, it.value)
          deletedAnswers.remove(it.key)
        }
        removed.map { fieldCode ->
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
}
