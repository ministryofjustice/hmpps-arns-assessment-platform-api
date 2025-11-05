package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import kotlin.reflect.KClass

interface AggregateState <A: Aggregate<A>> {
  val aggregates: MutableList<AggregateEntity<A>>
  val type: KClass<out A>
}
