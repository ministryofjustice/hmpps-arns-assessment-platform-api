package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

interface CommandExecutor {
  fun execute(request: CommandExecutorRequest): CommandExecutorResult
}
