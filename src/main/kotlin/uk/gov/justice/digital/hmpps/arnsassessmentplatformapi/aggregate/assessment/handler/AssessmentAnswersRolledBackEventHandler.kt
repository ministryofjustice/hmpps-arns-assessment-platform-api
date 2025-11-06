package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.Clock
import java.time.LocalDateTime

@Component
class AssessmentAnswersRolledBackEventHandler(
  private val clock: Clock,
  private val stateService: StateService,
) : AssessmentEventHandler<AssessmentAnswersRolledBackEvent>(stateService) {

  override val eventType = AssessmentAnswersRolledBackEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersRolledBackEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.get()

    val previousState = stateService.ForType(AssessmentAggregate::class).fetchStateForExactPointInTime(
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

    aggregate.data.timeline.add(
      TimelineItem(
        timestamp = event.createdAt,
        details = "Rolled back ${answersAdded.size + answersRemoved.size} answers",
      ),
    )

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = LocalDateTime.now(clock)
      numberOfEventsApplied += 1
    }

    return state
  }
}
