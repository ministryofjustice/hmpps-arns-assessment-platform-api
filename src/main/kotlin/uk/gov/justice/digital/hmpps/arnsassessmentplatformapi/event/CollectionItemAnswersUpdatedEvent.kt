package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import java.util.UUID

data class CollectionItemAnswersUpdatedEvent(
  val collectionItemUuid: UUID,
  val added: Map<String, List<String>>,
  val removed: List<String>,
  override val timeline: Timeline?,
) : Event
