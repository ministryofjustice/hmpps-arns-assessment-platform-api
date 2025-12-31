package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService

@Service
class CommandDispatcher(
  private val commandBus: CommandBus,
  private val auditService: AuditService,
) {
  @Retryable(
    value = [ObjectOptimisticLockingFailureException::class],
    maxAttempts = 3,
    backoff = Backoff(50),
  )
  fun dispatch(commands: List<Command>) = commandBus.dispatch(commands)
    .also { commands.forEach { command -> if (command is RequestableCommand) auditService.audit(command) } }
}
