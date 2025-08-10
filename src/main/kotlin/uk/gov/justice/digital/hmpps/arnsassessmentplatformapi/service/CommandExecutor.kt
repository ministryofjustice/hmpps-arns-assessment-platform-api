package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.CommandRequest

interface CommandExecutor {
  fun executeCommands(request: CommandRequest)
}
