package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Answers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Properties
import java.util.UUID

data class CollectionItem(
  val uuid: UUID,
  val answers: Answers,
  val properties: Properties,
  val collections: MutableList<Collection>,
) {
  fun findCollection(id: UUID): Collection? = collections.firstOrNull { it.uuid == id }
    ?: collections.firstNotNullOfOrNull { collection -> collection.items.firstNotNullOfOrNull { it.findCollection(id) } }
}
