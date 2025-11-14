package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline

data class AssessmentPropertiesUpdatedEvent(
  val added: Map<String, List<String>>,
  val removed: List<String>,
  override val timeline: Timeline?,
) : Event
