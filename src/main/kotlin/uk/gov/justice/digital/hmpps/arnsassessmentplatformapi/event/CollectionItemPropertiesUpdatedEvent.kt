package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionItemPropertiesUpdatedEvent(
  val collectionItemUuid: UUID,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event
