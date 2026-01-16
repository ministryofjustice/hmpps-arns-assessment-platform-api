package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.handler

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class AssessmentAnswersRolledBackEventHandler(
  private val clock: Clock,
  @param:Lazy private val stateService: StateService,
) : ValuesEventHandler<AssessmentAnswersRolledBackEvent> {

  override val eventType = AssessmentAnswersRolledBackEvent::class
  override val stateType = ValuesState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersRolledBackEvent>,
    state: ValuesState,
  ): ValuesState {
    val previousState = stateService.stateForType(ValuesAggregate::class).fetchOrCreateStateForExactPointInTime(
      event.assessment,
      event.data.rolledBackTo,
    ) as ValuesState

    val previousAnswers = previousState.getForRead().data.answers

    state.getForWrite().apply {
      for ((id, oldHistory) in previousAnswers) {
        data.addAnswer(id, oldHistory.latestValue)
      }

      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
