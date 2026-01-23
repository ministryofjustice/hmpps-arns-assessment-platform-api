package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.util.UUID

data class CollectionItemReorderedEvent(
  val collectionItemUuid: UUID,
  val index: Int,
  val previousIndex: Int,
  override val timeline: Timeline?,
  val collectionName: String,
) : Event
