package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.RollbackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class RollbackEventHandler(
  private val clock: Clock,
  @param:Lazy private val stateService: StateService,
) : AssessmentEventHandler<RollbackEvent> {

  override val eventType = RollbackEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventEntity<RollbackEvent>,
    state: AssessmentState,
  ): AssessmentState {
    val aggregate = state.getForWrite()

    val previousState = stateService.stateForType(AssessmentAggregate::class).fetchOrCreateStateForExactPointInTime(
      event.assessment,
      event.data.rolledBackTo,
    ) as AssessmentState

    aggregate.apply { data = previousState.getForWrite().data.clone() }

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
