package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService

@Service
class CommandBus(
  private val registry: CommandHandlerRegistry,
  private val auditService: AuditService,
) {
  fun dispatch(command: Command) = registry.getHandlerFor(command::class).execute(command)
    .also { if (command is RequestableCommand) auditService.audit(command) }

  @Transactional
  fun dispatch(commands: List<Command>) = CommandsResponse(commands.map { CommandResponse(it, dispatch(it)) })
}
