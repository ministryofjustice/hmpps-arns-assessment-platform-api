package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import kotlin.reflect.KClass

interface AggregateType {
  val aggregateType: String
  val updatesOn: Set<KClass<out Event>>
  val createsOn: Set<KClass<out Event>>
  val getInstance: () -> Aggregate
}
