package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

interface ValuesEventHandler<E : Event> : EventHandler<E, ValuesState> {
  override fun handle(event: EventEntity<E>, state: ValuesState): ValuesState
}
