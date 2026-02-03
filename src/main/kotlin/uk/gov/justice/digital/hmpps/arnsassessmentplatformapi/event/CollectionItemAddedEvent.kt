package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID

data class CollectionItemAddedEvent(
  val collectionItemUuid: UUID,
  val collectionUuid: UUID,
  val answers: Map<String, Value>,
  val properties: Map<String, Value>,
  val index: Int?,

) : Event
