package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity

class AssessmentState(
  override val aggregates: MutableList<AggregateEntity<AssessmentAggregate>> = mutableListOf(),
) : AggregateState<AssessmentAggregate> {
  override val type = AssessmentAggregate::class

  constructor(aggregate: AggregateEntity<AssessmentAggregate>) : this() {
    aggregates.add(aggregate)
  }

  fun getLatest() = aggregates.sortedWith(
    compareBy<AggregateEntity<AssessmentAggregate>> { it.eventsTo }
      .thenByDescending { it.numberOfEventsApplied },
  ).last()

  fun getForUpdate() = getLatest().takeIf { it.numberOfEventsApplied < 50 } ?: getLatest().clone().also { aggregates.add(it) }
}
