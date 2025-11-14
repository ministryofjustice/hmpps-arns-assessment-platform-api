package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

data class Timeline(
  val type: String,
  val data: Map<String, Any>,
) {
  fun item(event: EventEntity<*>) = TimelineItem(
    type = type,
    data = data,
    createdAt = event.createdAt,
  )
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Command {
  val timeline: Timeline?
}
