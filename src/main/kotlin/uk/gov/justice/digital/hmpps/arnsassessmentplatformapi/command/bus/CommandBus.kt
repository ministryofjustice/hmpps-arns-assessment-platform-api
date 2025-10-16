package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandlerRegistry
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.request.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService

@Service
class CommandBus(
  private val registry: CommandHandlerRegistry,
  private val auditService: AuditService,
) {
  fun dispatch(command: Command) = registry.getHandlerFor(command::class).execute(command)
    .also { if (command is RequestableCommand) auditService.audit(command) }
}
