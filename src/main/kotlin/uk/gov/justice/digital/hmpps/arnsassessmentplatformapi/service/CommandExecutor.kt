package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

interface CommandExecutor {
  fun execute(request: CommandRequest): List<EventEntity>
}
