package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntityView
import java.util.UUID
import kotlin.reflect.KClass

typealias State = MutableMap<KClass<out Aggregate<*>>, AggregateState<*>>
typealias StateCollection = MutableMap<UUID, State>

interface AggregateState<A : Aggregate<A>> {
  val aggregates: MutableList<AggregateEntity<A>>
  val type: KClass<out A>

  fun getForRead(): AggregateEntityView<out AggregateView>
  fun getForWrite(clock: Clock): AggregateEntity<A>
  fun getLatest(): AggregateEntity<A>
}
