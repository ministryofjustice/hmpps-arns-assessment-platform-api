package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.collection

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemoved
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class RemoveCollectionItemHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<RemoveCollectionItem> {
  override val type = RemoveCollectionItem::class
  override fun handle(command: RemoveCollectionItem): CommandSuccessCommandResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = CollectionItemRemoved(
            collectionUuid = collectionUuid,
            index = index,
          ),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
