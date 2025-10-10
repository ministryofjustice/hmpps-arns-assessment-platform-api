package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers.AggregateResponseMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers.AssessmentVersionMapper
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import java.awt.List
import kotlin.test.assertIs

class AggregateResponseMapperRegistryTest {
  @Test
  fun `it returns a response for a registered type`() {
    val mapperRegistry = AggregateResponseMapperRegistry(listOf(AssessmentVersionMapper()))

    val aggregate = AssessmentVersionAggregate()

    assertIs<AssessmentVersionResponse>(mapperRegistry.createResponseFrom(aggregate))
  }

  @Test
  fun `it throws when the aggregate type is not registered`() {
    val mapperRegistry = AggregateResponseMapperRegistry(emptyList())

    val aggregate = AssessmentVersionAggregate()

    assertThrows<MapperNotImplementedException> {
      mapperRegistry.createResponseFrom(aggregate)
    }
  }

  @Test
  fun `it throws when duplicate mappers are registered for an aggregate type`() {
    val mappers = List(2) {
      object : AggregateResponseMapper<AssessmentVersionAggregate> {
        override val aggregateType = AssessmentVersionAggregate::class

        override fun createResponseFrom(aggregate: AssessmentVersionAggregate): AggregateResponse = {} as AggregateResponse
      }
    }

    assertThrows<DuplicateMapperImplementedException> {
      AggregateResponseMapperRegistry(mappers)
    }
  }
}
