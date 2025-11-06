package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.reflect.KClass

interface EventHandler<E : Event, S : AggregateState<out Aggregate<*>>> {
  val eventType: KClass<out E>
  val stateType: KClass<out S>

  fun execute(event: EventEntity<E>, stateOverride: S?): S
}
