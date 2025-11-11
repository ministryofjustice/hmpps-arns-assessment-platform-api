package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model

import java.time.LocalDateTime
import java.util.UUID

data class Collection(
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val name: String,
  val items: MutableList<CollectionItem>,
) {
  fun findItem(id: UUID): CollectionItem? = items.firstOrNull { it.uuid == id }
    ?: items.firstNotNullOfOrNull { item -> item.collections.firstNotNullOfOrNull { it.findItem(id) } }

  fun removeItem(id: UUID): Boolean = items.removeIf { it.uuid == id } ||
    items.any { item -> item.collections.any { it.removeItem(id) } }

  fun reorderItem(id: UUID, targetIndex: Int): Boolean = items.indexOfFirst { it.uuid == id }.takeIf { it != -1 }?.let {
    items.add(targetIndex.coerceIn(0, items.size), items.removeAt(it))
    true
  } ?: items.any { item -> item.collections.any { it.reorderItem(id, targetIndex) } }
}
