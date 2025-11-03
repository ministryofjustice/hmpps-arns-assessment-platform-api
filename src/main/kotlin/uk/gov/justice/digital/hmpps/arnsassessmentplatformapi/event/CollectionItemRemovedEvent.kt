package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionItemRemovedEvent(
  val collectionItemUuid: UUID,
) : Event
