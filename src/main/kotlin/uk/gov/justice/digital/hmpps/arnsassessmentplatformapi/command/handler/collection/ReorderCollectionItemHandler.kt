package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.collection

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReordered
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class ReorderCollectionItemHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<ReorderCollectionItem> {
  override val type = ReorderCollectionItem::class
  override fun handle(command: ReorderCollectionItem): CommandSuccessCommandResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = CollectionItemReordered(
            collectionUuid = collectionUuid,
            index = index,
            previousIndex = previousIndex,
          ),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
