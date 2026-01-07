package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.values

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntityView

class ValuesState(
  override val aggregates: MutableList<AggregateEntity<ValuesAggregate>> = mutableListOf(),
) : AggregateState<ValuesAggregate> {
  override val type = ValuesAggregate::class

  constructor(aggregate: AggregateEntity<ValuesAggregate>) : this() {
    aggregates.add(aggregate)
  }

  private fun getLatest() = aggregates.sortedWith(
    compareBy<AggregateEntity<ValuesAggregate>> { it.eventsTo }
      .thenByDescending { it.numberOfEventsApplied },
  ).last()

  fun getForRead(): AggregateEntityView<out ValuesAggregateView> = getLatest()

  fun getForWrite() = getLatest().takeIf { it.numberOfEventsApplied < 50 } ?: getLatest().clone().also { aggregates.add(it) }
}
