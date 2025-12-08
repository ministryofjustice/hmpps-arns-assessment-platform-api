package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class AddCollectionItemCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
) : CommandHandler<AddCollectionItemCommand> {
  override val type = AddCollectionItemCommand::class

  override fun handle(command: AddCollectionItemCommand): AddCollectionItemCommandResult {
    UpdateAssessmentAnswersCommandHandler.validateAnswers(command.answers)

    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = CollectionItemAddedEvent(collectionItemUuid, collectionUuid, answers, properties, index, timeline),
      )
    }

    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    return AddCollectionItemCommandResult(command.collectionItemUuid)
  }
}
