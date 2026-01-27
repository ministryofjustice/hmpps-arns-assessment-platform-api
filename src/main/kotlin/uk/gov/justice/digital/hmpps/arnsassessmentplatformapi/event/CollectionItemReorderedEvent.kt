package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import java.util.UUID
import kotlin.collections.set

data class CollectionItemReorderedEvent(
  val collectionItemUuid: UUID,
  val index: Int,
) : Event
