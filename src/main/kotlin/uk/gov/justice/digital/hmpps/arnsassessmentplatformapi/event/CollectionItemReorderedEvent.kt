package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import java.util.UUID

data class CollectionItemReorderedEvent(
  val collectionItemUuid: UUID,
  val index: Int,
  override val timeline: TimelineItem?,
) : Event
