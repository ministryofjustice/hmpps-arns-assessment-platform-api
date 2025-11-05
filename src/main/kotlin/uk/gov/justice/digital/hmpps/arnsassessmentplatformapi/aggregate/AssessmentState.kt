package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity

class AssessmentState(
  override val aggregates: MutableList<AggregateEntity<AssessmentAggregate>> = mutableListOf(),
): AggregateState<AssessmentAggregate> {
  override val type = AssessmentAggregate::class

  constructor(aggregate: AggregateEntity<AssessmentAggregate>) : this() { aggregates.add(aggregate) }

  fun current(): AggregateEntity<AssessmentAggregate> =
    (aggregates.last().takeIf { it.numberOfEventsApplied < 50 }
      ?: aggregates.last().clone().also { aggregates.add(it) })
}
