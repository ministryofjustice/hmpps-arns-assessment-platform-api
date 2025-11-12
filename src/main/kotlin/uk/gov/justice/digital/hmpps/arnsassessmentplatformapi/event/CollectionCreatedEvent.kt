package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import java.util.UUID

data class CollectionCreatedEvent(
  val collectionUuid: UUID,
  val name: String,
  val parentCollectionItemUuid: UUID?,
  override val timeline: TimelineItem?,
) : Event
