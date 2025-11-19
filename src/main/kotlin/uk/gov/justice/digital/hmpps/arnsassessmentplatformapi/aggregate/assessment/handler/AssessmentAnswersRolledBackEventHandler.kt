package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class AssessmentAnswersRolledBackEventHandler(
  private val clock: Clock,
  @param:Lazy private val stateService: StateService,
) : AssessmentEventHandler<AssessmentAnswersRolledBackEvent> {

  override val eventType = AssessmentAnswersRolledBackEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersRolledBackEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    val previousState = stateService.stateForType(AssessmentAggregate::class).fetchOrCreateStateForExactPointInTime(
      event.assessment,
      event.data.rolledBackTo,
    ) as AssessmentState

    val currentAnswers = aggregate.data.answers
    val previousAnswers = previousState.get().data.answers

    val answersAdded = buildMap {
      for ((key, oldValue) in previousAnswers) {
        if (currentAnswers[key] != oldValue) {
          put(key, oldValue)
        }
      }
    }

    val answersRemoved = currentAnswers.keys.filter { !previousAnswers.contains(it) }

    AssessmentAnswersUpdatedEventHandler.updateAnswers(state, answersAdded, answersRemoved)

    aggregate.data.apply {
      collaborators.add(event.user)
      event.data.timeline?.let { timeline.add(it.item(event)) }
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
