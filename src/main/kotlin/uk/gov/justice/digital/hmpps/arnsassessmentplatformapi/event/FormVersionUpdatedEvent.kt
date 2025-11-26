package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline

data class FormVersionUpdatedEvent(
  val version: String,
  override val timeline: Timeline?,
) : Event
