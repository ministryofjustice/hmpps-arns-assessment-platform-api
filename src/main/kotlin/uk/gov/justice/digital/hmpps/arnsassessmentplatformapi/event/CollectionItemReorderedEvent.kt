package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionItemReorderedEvent(
  val collectionItemUuid: UUID,
  val index: Int,
) : Event
