package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

abstract class AssessmentEventHandler<E : Event>(
  private val stateService: StateService,
) : EventHandler<E, AssessmentState> {
  abstract fun handle(event: EventEntity<E>, state: AssessmentState): AssessmentState

  final override fun execute(event: EventEntity<E>, stateOverride: AssessmentState?): AssessmentState = stateOverride ?: stateService.ForType(AssessmentAggregate::class).fetchLatestState(event.assessment) as AssessmentState
}
