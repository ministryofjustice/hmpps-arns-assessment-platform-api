package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value
import java.util.UUID
import kotlin.collections.set

data class CollectionItemAnswersUpdatedEvent(
  val collectionItemUuid: UUID,
  val added: Map<String, Value>,
  val removed: List<String>,
) : Event
