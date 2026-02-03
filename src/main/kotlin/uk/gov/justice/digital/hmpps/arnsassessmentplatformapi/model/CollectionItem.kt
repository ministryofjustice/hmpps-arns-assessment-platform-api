package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Answers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AnswersView
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Properties
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.PropertiesView
import java.time.LocalDateTime
import java.util.UUID

interface CollectionItemView {
  val uuid: UUID
  val createdAt: LocalDateTime
  val updatedAt: LocalDateTime
  val answers: AnswersView
  val properties: PropertiesView
  val collections: List<CollectionView>

  fun findCollection(id: UUID): Collection?
}

data class CollectionItem(
  override val uuid: UUID,
  override val createdAt: LocalDateTime,
  override var updatedAt: LocalDateTime,
  override val answers: Answers,
  override val properties: Properties,
  override val collections: MutableList<Collection>,
) : CollectionItemView {
  override fun findCollection(id: UUID): Collection? = collections.firstOrNull { it.uuid == id }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(id) } }

  fun findCollectionWithItem(collectionItemUuid: UUID): Collection? = collections.firstNotNullOfOrNull { collection ->
    val item = collection.items.firstOrNull { item -> item.uuid == collectionItemUuid }

    if (item != null) {
      collection
    } else {
      collection.items.firstNotNullOfOrNull { item -> item.findCollectionWithItem(collectionItemUuid) }
    }
  }
}
