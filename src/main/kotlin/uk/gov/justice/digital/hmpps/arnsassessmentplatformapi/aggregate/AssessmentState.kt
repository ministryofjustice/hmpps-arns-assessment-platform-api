package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity

class AssessmentState(
  val aggregates: MutableList<AggregateEntity> = mutableListOf(),
) {
  constructor(aggregate: AggregateEntity) : this() { aggregates.add(aggregate) }

  fun current(): AggregateEntity =
    aggregates.last().takeIf { it.numberOfEventsApplied < 50 }
      ?: aggregates.last().clone().also { aggregates.add(it) }
}
