package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.collection

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService

@Component
class CreateCollectionHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
) : CommandHandler<CreateCollection> {
  override val type = CreateCollection::class
  override fun handle(command: CreateCollection): CommandSuccessCommandResult {
    val assessment = assessmentService.findByUuid(command.assessmentUuid)
    eventBus.add(
      with(command) {
        EventEntity(
          user = user,
          assessment = assessment,
          data = CollectionCreated(name),
        )
      },
    )
    return CommandSuccessCommandResult()
  }
}
