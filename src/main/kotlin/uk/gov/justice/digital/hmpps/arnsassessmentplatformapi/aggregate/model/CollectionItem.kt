package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.model

import java.util.UUID

data class CollectionItem(
  val uuid: UUID,
  val answers: MutableMap<String, List<String>>,
  val collections: MutableList<Collection>,
) {
  fun findCollection(id: UUID): Collection? = collections.firstOrNull { it.uuid == id }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(id) } }
}
