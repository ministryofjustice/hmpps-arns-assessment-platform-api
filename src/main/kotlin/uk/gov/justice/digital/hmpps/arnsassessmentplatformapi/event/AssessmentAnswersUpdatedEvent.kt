package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline

data class AssessmentAnswersUpdatedEvent(
  val added: Map<String, List<String>>,
  val removed: List<String>,
  override val timeline: Timeline?,
) : Event
