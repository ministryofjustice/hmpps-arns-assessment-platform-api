package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class ReorderCollectionItemCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<ReorderCollectionItemCommand> {
  override val type = ReorderCollectionItemCommand::class
  override fun handle(command: ReorderCollectionItemCommand): CommandSuccessCommandResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = CollectionItemReorderedEvent(
            collectionItemUuid = collectionItemUuid,
            index = index,
          ),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
