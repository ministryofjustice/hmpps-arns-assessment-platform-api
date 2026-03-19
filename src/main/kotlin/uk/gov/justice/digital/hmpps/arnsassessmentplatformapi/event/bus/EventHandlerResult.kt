package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver

data class EventHandlerResult<S : AggregateState<out Aggregate<*>>>(
  val state: S,
  val timeline: TimelineResolver? = null,
)
