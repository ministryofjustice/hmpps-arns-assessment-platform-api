package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionCreatedEvent(
  val collectionUuid: UUID,
  val name: String,
  val parentCollectionItemUuid: UUID?,

) : Event
