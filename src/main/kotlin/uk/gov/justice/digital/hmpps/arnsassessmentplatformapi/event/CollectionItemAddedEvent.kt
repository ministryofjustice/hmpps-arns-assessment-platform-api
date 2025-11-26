package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.util.UUID

data class CollectionItemAddedEvent(
  val collectionItemUuid: UUID,
  val collectionUuid: UUID,
  val answers: Map<String, List<String>>,
  val properties: Map<String, List<String>>,
  val index: Int?,
  override val timeline: Timeline?,
) : Event
