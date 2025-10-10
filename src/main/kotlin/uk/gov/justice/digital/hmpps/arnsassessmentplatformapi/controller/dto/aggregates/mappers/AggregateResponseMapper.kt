package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AggregateResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate
import kotlin.reflect.KClass

interface AggregateResponseMapper<T : Aggregate> {
  val aggregateType: KClass<*>
  fun createResponseFrom(aggregate: T): AggregateResponse
}
