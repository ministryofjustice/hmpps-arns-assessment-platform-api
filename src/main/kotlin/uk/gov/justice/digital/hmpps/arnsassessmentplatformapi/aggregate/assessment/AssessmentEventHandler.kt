package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventProto

interface AssessmentEventHandler<E : Event> : EventHandler<E, AssessmentState> {
  override fun handle(event: EventProto<E>, state: AssessmentState): EventHandlerResult<AssessmentState>
}
