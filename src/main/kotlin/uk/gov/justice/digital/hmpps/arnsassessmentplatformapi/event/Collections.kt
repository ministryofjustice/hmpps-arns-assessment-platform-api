package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionCreated(
  val name: String,
) : Event {
  val collectionUuid: UUID = UUID.randomUUID()
}

data class ChildCollectionCreated(
  val name: String,
  val parentCollectionUuid: UUID,
) : Event {
  val collectionUuid: UUID = UUID.randomUUID()
}

data class CollectionItemAdded(
  val collectionUuid: UUID,
  val answers: Map<String, List<String>>,
  val index: Int?,
) : Event

data class CollectionItemUpdated(
  val collectionUuid: UUID,
  val index: Int,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event

data class CollectionItemRemoved(
  val collectionUuid: UUID,
  val index: Int,
) : Event

data class CollectionItemReordered(
  val collectionUuid: UUID,
  val index: Int,
  val previousIndex: Int,
) : Event
