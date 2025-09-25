package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.CommandExecutorResultException
import java.util.UUID

class CommandExecutorResult(
  val events: List<EventEntity> = listOf(),
  private val assessmentUuid: UUID? = null,
) {
  init {
    if (events.isNotEmpty() && assessmentUuid == null) {
      throw CommandExecutorResultException("Assessment UUID must be present if events exist")
    }
  }

  fun isOk() = events.isNotEmpty() && assessmentUuid != null
  fun getAssessmentUuid() = if (events.isNotEmpty()) assessmentUuid else null

  fun mergeWith(other: CommandExecutorResult): CommandExecutorResult {
    if (assessmentUuid != null && other.assessmentUuid != null && assessmentUuid != other.assessmentUuid) {
      throw CommandExecutorResultException("Conflicting assessment UUIDs while merging results: $assessmentUuid vs $other.assessmentUuid")
    }

    val mergedEvents = when {
      events.isEmpty() -> other.events
      other.events.isEmpty() -> events
      else -> events.plus(other.events)
    }

    return CommandExecutorResult(mergedEvents, assessmentUuid ?: other.assessmentUuid)
  }
}
