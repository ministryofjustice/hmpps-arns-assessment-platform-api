package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID

data class CollectionItemAnswersUpdatedEvent(
  val collectionItemUuid: UUID,
  val added: Map<String, List<String>>,
  val removed: List<String>,
) : Event
