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
      collections = aggregate.data.collections.toRenderedCollection(),
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
)

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
  val index: Int,
  val answers: List<RenderedValue>,
  val properties: List<RenderedValue>,
  val collections: List<RenderedCollection>,
)

data class RenderedCollection(
  val name: String,
  val items: List<RenderedCollectionItem>,
)

fun List<Collection>.toRenderedCollection(): List<RenderedCollection> = map { collection ->
  RenderedCollection(
    name = collection.name,
    items = collection.items.mapIndexed { index, item ->
      RenderedCollectionItem(
        index = index,
        answers = item.answers.toRenderedValues(),
        properties = item.properties.toRenderedValues(),
        collections = item.collections.toRenderedCollection(),
      )
    },
  )
}
