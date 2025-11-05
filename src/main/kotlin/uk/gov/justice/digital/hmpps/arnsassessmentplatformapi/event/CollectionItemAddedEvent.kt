package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionItemAddedEvent(
  val collectionItemUuid: UUID,
  val collectionUuid: UUID,
  val answers: Map<String, List<String>>,
  val properties: Map<String, List<String>>,
  val index: Int?,
) : Event
