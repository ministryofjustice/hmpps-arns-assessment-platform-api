package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import java.time.LocalDateTime

data class CommandTimeline(
  val type: String,
  val data: Map<String, Any>,
) {
  fun into() = TimelineItem(type, LocalDateTime.now(), data)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Command {
  val timeline: CommandTimeline?
}
