package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers.AggregateResponseMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate

class MapperNotImplementedException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Failed to convert aggregate to response",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )

@Component
class AggregateResponseMapperRegistry(val mappers: List<AggregateResponseMapper>) {
  private val byType = mappers.associateBy { it.aggregateType }

  fun intoResponse(type: String, aggregate: Aggregate): AggregateResponse = byType[type]?.intoResponse(aggregate)
    ?: throw MapperNotImplementedException("No mapper has been implemented for type: $type, supported types: ${mappers.joinToString { it.aggregateType }}")
}
