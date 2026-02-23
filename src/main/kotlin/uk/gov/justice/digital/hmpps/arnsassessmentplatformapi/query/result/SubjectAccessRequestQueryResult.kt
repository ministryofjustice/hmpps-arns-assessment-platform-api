package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregateView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.MultiValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntityView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import java.time.LocalDateTime

data class SubjectAccessRequestAssessmentVersion(
  val assessmentType: String,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val answers: List<RenderedValue>,
  val properties: List<RenderedValue>,
  val collections: List<RenderedCollection>,
  val identifiers: Map<IdentifierType, String>,
) {
  val hasAnswersOrProperties: Boolean get() = answers.isNotEmpty() || properties.isNotEmpty()

  companion object {
    fun from(
      aggregate: AggregateEntityView<out AssessmentAggregateView>,
      assessment: AssessmentEntity,
    ): SubjectAccessRequestAssessmentVersion = SubjectAccessRequestAssessmentVersion(
      assessmentType = assessment.type,
      createdAt = aggregate.eventsFrom,
      updatedAt = aggregate.eventsTo,
      answers = aggregate.data.answers.toRenderedValues(),
      properties = aggregate.data.properties.toRenderedValues(),
      collections = aggregate.data.collections.toFlattenedRenderedCollections(),
      identifiers = assessment.identifiersMap(),
    )
  }
}

data class SubjectAccessRequestQueryResult(
  val results: List<SubjectAccessRequestAssessmentVersion>,
) : QueryResult

data class RenderedValue(
  val key: String,
  val single: String? = null,
  val multiple: List<String>? = null,
) {
  val hasSingle: Boolean get() = single != null
  val hasMultiple: Boolean get() = multiple != null
}

fun Map<String, Value>.toRenderedValues(): List<RenderedValue> = map { (key, value) ->
  when (value) {
    is SingleValue ->
      RenderedValue(
        key = key,
        single = value.value,
      )

    is MultiValue ->
      RenderedValue(
        key = key,
        multiple = value.values,
      )
  }
}

data class RenderedCollectionItem(
  val name: String,
  val answers: List<RenderedValue>,
  val properties: List<RenderedValue>,
)

data class RenderedCollection(
  val name: String,
  val items: List<RenderedCollectionItem>,
)

private fun Collection.flatten(
  path: List<String>,
): List<RenderedCollection> {
  val collectionPath = path + name

  val current = RenderedCollection(
    name = collectionPath.joinToString("/"),
    items = items.mapIndexed { index, item ->
      val itemPath = collectionPath + (index + 1).toString()

      RenderedCollectionItem(
        name = itemPath.joinToString("/"),
        answers = item.answers.toRenderedValues(),
        properties = item.properties.toRenderedValues(),
      )
    },
  )

  val nested =
    items.flatMapIndexed { index, item ->
      val itemPath = collectionPath + (index + 1).toString()

      item.collections.flatMap { childCollection ->
        childCollection.flatten(itemPath)
      }
    }

  return listOf(current) + nested
}

fun List<Collection>.toFlattenedRenderedCollections(): List<RenderedCollection> = flatMap { it.flatten(emptyList()) }
