package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.EventDTO
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem

data class DataDeletionDataResponse(
  val events: List<EventDTO>,
  val timeline: List<TimelineItem>,
)
