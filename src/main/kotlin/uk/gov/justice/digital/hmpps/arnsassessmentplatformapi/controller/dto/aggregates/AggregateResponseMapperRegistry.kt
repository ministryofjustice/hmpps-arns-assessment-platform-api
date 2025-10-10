package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.AssessmentPlatformException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers.AggregateResponseMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.Aggregate
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class MapperNotImplementedException(developerMessage: String) :
  AssessmentPlatformException(
    message = "Failed to convert aggregate to response",
    developerMessage = developerMessage,
    statusCode = HttpStatus.BAD_REQUEST,
  )

class DuplicateMapperImplementedException(duplicates: Set<KClass<*>>) :
  AssessmentPlatformException(
    message = "Duplicate mappers implemented",
    developerMessage = "Multiple mappers are registered for: ${
      duplicates.joinToString { it.qualifiedName ?: it.simpleName ?: "<anonymous>" }
    }",
    statusCode = HttpStatus.SERVICE_UNAVAILABLE,
  )

@Component
class AggregateResponseMapperRegistry(
  mappers: List<AggregateResponseMapper<*>>,
) {
  private val byType: Map<KClass<*>, AggregateResponseMapper<*>> =
    mappers.groupBy { it.aggregateType }
      .also { grouped ->
        val duplicates = grouped.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
          throw DuplicateMapperImplementedException(duplicates)
        }
      }
      .mapValues { (_, v) -> v.single() }

  fun <T : Aggregate> createResponseFrom(aggregate: T): AggregateResponse = mapperFor(aggregate).createResponseFrom(aggregate)

  fun <T : Aggregate> mapperFor(aggregate: T): AggregateResponseMapper<T> {
    val k = aggregate::class
    val mapper = byType[k]
      ?: byType.entries.firstOrNull { (registered, _) -> k.isSubclassOf(registered) }?.value
      ?: throw MapperNotImplementedException(
        "No mapper implemented for ${k.qualifiedName}. Supported: ${supportedTypes()}",
      )
    @Suppress("UNCHECKED_CAST")
    return mapper as AggregateResponseMapper<T>
  }

  private fun supportedTypes(): String = byType.keys.joinToString { it.qualifiedName ?: it.simpleName ?: "<anonymous>" }
}
