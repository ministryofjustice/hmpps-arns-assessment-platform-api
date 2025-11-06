package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

interface AssessmentEventHandler<E : Event> : EventHandler<E, AssessmentState> {
  override fun handle(event: EventEntity<E>, state: AssessmentState): AssessmentState
}
