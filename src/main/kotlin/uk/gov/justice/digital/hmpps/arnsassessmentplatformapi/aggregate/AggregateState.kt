package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import kotlin.reflect.KClass

typealias State = MutableMap<KClass<out Aggregate<*>>, AggregateState<*>>

interface AggregateState <A: Aggregate<A>> {
  val aggregates: MutableList<AggregateEntity<A>>
  val type: KClass<out A>
}
