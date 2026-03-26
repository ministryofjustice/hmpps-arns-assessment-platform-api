package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.AnswerNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class AssessmentAnswersUpdatedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<AssessmentAnswersUpdatedEvent> {
  override val eventType = AssessmentAnswersUpdatedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersUpdatedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    updateAnswers(state, event.data.added, event.data.removed)
    state.getForWrite(clock).data.apply {
      collaborators.add(event.user.uuid)
    }

    state.getForWrite(clock).apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return EventHandlerResult(
      state = state,
      timeline = TimelineEntity.resolver(
        event,
        mapOf(
          "added" to event.data.added.keys,
          "removed" to event.data.removed,
        ),
      ),
    )
  }

  private fun updateAnswers(state: AssessmentState, added: Map<String, Value>, removed: List<String>) {
    with(state.getForWrite(clock)) {
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
