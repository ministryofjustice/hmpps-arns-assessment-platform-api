package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.common

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FieldType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.formconfig.Option
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Field
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.oasys.datamapping.exception.InvalidMappingException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value as PersistedAnswer

abstract class Answer {
  open val value: String?
    get() = throw InvalidMappingException(
      "Invalid use of '.value' on a ${this::class.simpleName}",
    )

  open val values: List<String>?
    get() = throw InvalidMappingException(
      "Invalid use of '.values' on a ${this::class.simpleName}",
    )

  open val collection: List<Map<String, PersistedAnswer>>
    get() = throw InvalidMappingException(
      "Invalid use of '.collection' on a ${this::class.simpleName}",
    )
}

class SingleValueAnswer(
  override val value: String?,
) : Answer()

class MultipleValuesAnswer(
  override val values: List<String>?,
) : Answer()

class CollectionAnswer(
  override val collection: List<Map<String, PersistedAnswer>>,
) : Answer()

class AnswersProvider(
  private val assessment: AssessmentAggregateView,
  private val config: FormConfig,
) {
  private var context: String? = null

  fun setContext(field: Field) {
    context = field.lower
  }

  fun answer(field: Field): Answer {
    setContext(field)

    return config.fields[context]
      // check if context is a field
      ?.let { fieldConfig ->
        val answer = assessment.answers[fieldConfig.code]
          ?: assessment.properties[fieldConfig.code]

        when (fieldConfig.type) {
          FieldType.CHECKBOX -> MultipleValuesAnswer(
            values = (answer as? MultiValue)?.values?.let { values -> values.takeUnless { it == listOf("") }.orEmpty() },
          )
          else -> SingleValueAnswer(
            (answer as? SingleValue)?.value,
          )
        }
      }
      // context is not a field
      ?: throw InvalidMappingException(
        "Field $context does not exist in form config version ${config.version}",
      )
  }

  fun answer(collection: Collection): Answer {
    context = null

    if (config.fields.filter { it.value.collection == collection.name }.isEmpty()) {
      throw InvalidMappingException(
        "Collection ${collection.name} does not exist in form config version ${config.version}",
      )
    }

    return assessment.collections
      .find { it.name == collection.name }
      ?.let { collection ->
        CollectionAnswer(
          collection.items.map { it.answers },
        )
      }
      ?: CollectionAnswer(collection = emptyList())
  }

  fun get(value: Value): String {
    if (context == null) {
      throw InvalidMappingException(
        "Cannot obtain values without a field context. Call answer() first",
      )
    }

    val valueName = value.name

    if (config.fields[context]?.options?.contains(Option(valueName)) != true) {
      throw InvalidMappingException(
        "$valueName is not a valid option for field $context in form config version ${config.version}",
      )
    }

    return valueName
  }
}
