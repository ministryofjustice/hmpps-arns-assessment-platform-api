package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.SavedDraft
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentDraftService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.collections.ifEmpty

@Service
class TransactionalCommandDispatcher(
  private val commandBusFactory: CommandBusFactory,
  private val auditService: AuditService,
  private val assessmentDraftService: AssessmentDraftService,
) {
  @Transactional
  fun dispatch(commands: List<Command>, savedDraft: SavedDraft? = null) = commandBusFactory.create().dispatchAndPersist(commands)
    .also {
      commands.filterIsInstance<RequestableCommand>().ifEmpty { null }?.let { auditService.audit(it) }
      savedDraft?.let(assessmentDraftService::deleteAcknowledged)
    }
}
