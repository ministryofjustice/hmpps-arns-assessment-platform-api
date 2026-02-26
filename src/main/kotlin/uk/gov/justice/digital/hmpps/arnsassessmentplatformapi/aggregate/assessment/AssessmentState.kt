package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntityView

class AssessmentState(
  override val aggregates: MutableList<AggregateEntity<AssessmentAggregate>> = mutableListOf(),
) : AggregateState<AssessmentAggregate> {
  override val type = AssessmentAggregate::class

  constructor(aggregate: AggregateEntity<AssessmentAggregate>) : this() {
    aggregates.add(aggregate)
  }

  private fun getLatest() = aggregates.sortedWith(
    compareBy<AggregateEntity<AssessmentAggregate>> { it.eventsTo }
      .thenByDescending { it.numberOfEventsApplied },
  ).last()

  fun getForRead(): AggregateEntityView<out AssessmentAggregateView> = getLatest()

  fun getForWrite(clock: Clock) = getLatest().takeIf { it.numberOfEventsApplied < 50 } ?: getLatest().clone(clock).also { aggregates.add(it) }
}
