package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.san

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common.SanSectionMapping
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.service.OasysEquivalent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import kotlin.test.assertEquals
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value as PersistedValue

abstract class SectionMappingTest(
  private val sanSectionMapping: SanSectionMapping,
) {
  fun test(questionCode: Field, vararg scenarios: Given) {
    for ((scenarioNumber, scenario) in scenarios.withIndex()) {
      val assessmentAggregate = AssessmentAggregate()
      sanSectionMapping.map(scenario.oasysData, assessmentAggregate)
      assertEquals(scenario.expected, assessmentAggregate.answers[questionCode.lower], "Scenario ${scenarioNumber + 1} failed")
    }
  }
}

class Given {
  var oasysData: OasysEquivalent = mapOf()
  var expected: PersistedValue? = null

  constructor()

  constructor(field: String, value: String?) {
    this.oasysData = oasysData.plus(field to value)
  }

  constructor(field: String, value: Map<String, Any?>) {
    this.oasysData = oasysData.plus(field to value)
  }

  fun and(field: String, value: String?): Given {
    this.oasysData = oasysData.plus(field to value)
    return this
  }

  fun and(field: String, value: Map<String, Any?>): Given {
    this.oasysData = oasysData.plus(field to value)
    return this
  }

  fun expect(expected: String?): Given {
    this.expected = expected?.run(::SingleValue)
    return this
  }

  fun expect(vararg expected: String): Given {
    this.expected = when (expected.size) {
      1 -> SingleValue(expected[0])
      else -> MultiValue(expected.toList())
    }
    return this
  }

  fun expect(expected: Value): Given {
    this.expected = expected.name.let(::SingleValue)
    return this
  }

  fun expect(vararg expected: Value): Given {
    this.expected = when (expected.size) {
      1 -> SingleValue(expected[0].name)
      else -> MultiValue(expected.toList().map { it.name })
    }
    return this
  }
}
