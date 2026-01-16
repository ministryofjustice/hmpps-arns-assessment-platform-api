package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.ValuesState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values.model.ValueId
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
class AssessmentAnswersUpdatedEventHandler(
  private val clock: Clock,
) : ValuesEventHandler<AssessmentAnswersUpdatedEvent> {

  override val eventType = AssessmentAnswersUpdatedEvent::class
  override val stateType = ValuesState::class

  override fun handle(
    event: EventEntity<AssessmentAnswersUpdatedEvent>,
    state: ValuesState,
  ): ValuesState {
    state.getForWrite().apply {
      for ((key, value) in event.data.added) {
        data.addAnswer(ValueId.of(key), value)
      }
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return state
  }
}
