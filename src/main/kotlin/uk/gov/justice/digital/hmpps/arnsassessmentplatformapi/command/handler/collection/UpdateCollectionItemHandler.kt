package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.collection

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class UpdateCollectionItemHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<UpdateCollectionItem> {
  override val type = UpdateCollectionItem::class
  override fun handle(command: UpdateCollectionItem): CommandSuccessCommandResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = CollectionItemUpdated(
            collectionUuid = collectionUuid,
            index = index,
            added = added,
            removed = removed,
          ),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
