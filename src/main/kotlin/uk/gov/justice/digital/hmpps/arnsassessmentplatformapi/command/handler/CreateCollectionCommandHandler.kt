package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

@Component
class CreateCollectionCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
) : CommandHandler<CreateCollectionCommand> {
  override val type = CreateCollectionCommand::class
  override fun handle(command: CreateCollectionCommand): CreateCollectionCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findBy(UuidIdentifier(assessmentUuid)),
        data = CollectionCreatedEvent(collectionUuid, name, parentCollectionItemUuid, timeline),
      )
    }

    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    return CreateCollectionCommandResult(command.collectionUuid)
  }
}
