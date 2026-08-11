package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value as PersistedValue

abstract class SectionMappingTest(
  private val sectionMapping: SectionMapping,
  version: String,
) {
  private val formConfig = FormConfig(
    version,
    fields = mapOf(), // TODO: copy the real field config from the UI
  )

  fun test(questionCode: String, vararg scenarios: Given) {
    for ((scenarioNumber, scenario) in scenarios.withIndex()) {
      val answersProvider = AnswersProvider(scenario.assessment, formConfig)
      val result = sectionMapping.map(answersProvider)

      assertContains(result, questionCode, "Scenario ${scenarioNumber + 1} failed")
      assertEquals(scenario.expected, result[questionCode], "Scenario ${scenarioNumber + 1} failed")
    }
  }
}

class Given {
  var assessment: AssessmentAggregate = AssessmentAggregate()
  var expected: Any? = null

  constructor()

  constructor(field: Field, value: String?) {
    this.assessment.apply {
      value?.let { answers[field.lower] = SingleValue(it) }
    }
  }

  constructor(field: Field, value: Value) {
    this.assessment.apply {
      answers[field.lower] = SingleValue(value.name)
    }
  }

  constructor(field: Field, values: List<Value>) {
    this.assessment.apply {
      answers[field.lower] = MultiValue(values.map { it.name })
    }
  }

  fun and(field: Field, value: String?): Given {
    this.assessment.apply {
      value?.let { answers[field.lower] = SingleValue(it) }
    }
    return this
  }

  fun and(field: Field, value: Value): Given {
    this.assessment.apply {
      answers[field.lower] = SingleValue(value.name)
    }
    return this
  }

  fun and(field: Field, values: List<Value>): Given {
    this.assessment.apply {
      answers[field.lower] = MultiValue(values.map { it.name })
    }
    return this
  }

  fun expect(expected: Any?): Given {
    this.expected = expected
    return this
  }

  companion object {
    fun aCollectionOf(field: Field, collectionItems: List<Map<String, PersistedValue>>): Given = Given().apply {
      assessment.apply {
        collections.addLast(
          Collection(
            uuid = UUID.randomUUID(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            name = field.name,
            items = collectionItems.map { item ->
              CollectionItem(
                uuid = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                answers = item.toMutableMap(),
                properties = mutableMapOf(),
                collections = mutableListOf(),
              )
            }.toMutableList(),
          ),
        )
      }
    }
  }
}
