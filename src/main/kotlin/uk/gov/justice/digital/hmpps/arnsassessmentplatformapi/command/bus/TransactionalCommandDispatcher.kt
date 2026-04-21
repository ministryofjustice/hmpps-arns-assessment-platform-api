package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.collections.ifEmpty

@Service
class TransactionalCommandDispatcher(
  private val commandBusFactory: CommandBusFactory,
  private val auditService: AuditService,
) {
  @Transactional
  fun dispatch(commands: List<Command>) = commandBusFactory.create().dispatchAndPersist(commands)
    .also {
      commands.filterIsInstance<RequestableCommand>().ifEmpty { null }?.let { auditService.audit(it) }
    }
}
