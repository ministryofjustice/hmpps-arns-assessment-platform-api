package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Value

data class AssessmentAnswersUpdatedEvent(
  val added: Map<String, Value>,
  val removed: List<String>,
  override val timeline: Timeline?,
) : Event
