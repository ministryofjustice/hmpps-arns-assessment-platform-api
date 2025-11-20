package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.util.UUID

data class CollectionItemRemovedEvent(
  val collectionItemUuid: UUID,
  override val timeline: Timeline?,
) : Event
