package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.AnswerNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

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
    state.getForWrite().data.apply {
      collaborators.add(event.user)
      event.data.timeline?.let { timeline.add(it.item(event)) }
    }

    state.getForWrite().apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }

  companion object {
    fun updateAnswers(state: AssessmentState, added: Map<String, Value>, removed: List<String>) {
      with(state.getForWrite()) {
        added.entries.forEach {
          data.answers[it.key] = it.value
        }
        removed.forEach { fieldCode ->
          val removedValue = data.answers[fieldCode]
          if (removedValue != null) {
            data.answers.remove(fieldCode)
          } else {
            throw AnswerNotFoundException(fieldCode, uuid)
          }
        }
      }
    }
  }
}
