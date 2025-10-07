package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers.AssessmentVersionMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import kotlin.test.assertIs

class AggregateResponseMapperRegistryTest {
  @Test
  fun `it calls intoResponse for a registered type`() {
    val mapperRegistry = AggregateResponseMapperRegistry(listOf(AssessmentVersionMapper()))

    val aggregate = AssessmentVersionAggregate()

    assertIs<AssessmentVersionResponse>(mapperRegistry.intoResponse(aggregate))
  }

  @Test
  fun `it throws when the aggregate type is not registered`() {
    val mapperRegistry = AggregateResponseMapperRegistry(emptyList())

    val aggregate = AssessmentVersionAggregate()

    assertThrows<MapperNotImplementedException> {
      mapperRegistry.intoResponse(aggregate)
    }
  }
}
