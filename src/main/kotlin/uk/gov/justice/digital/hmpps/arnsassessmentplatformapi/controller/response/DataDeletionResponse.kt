package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlingException

data class DataDeletionResponse(
  val success: Boolean,
  val dryRun: Boolean,
  val exception: EventHandlingException? = null,
  val state: State = mutableMapOf(),
)
