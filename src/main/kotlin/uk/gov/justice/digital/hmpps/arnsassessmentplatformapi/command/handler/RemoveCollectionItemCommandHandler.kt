package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService

@Component
class RemoveCollectionItemCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
) : CommandHandler<RemoveCollectionItemCommand> {
  override val type = RemoveCollectionItemCommand::class
  override fun handle(command: RemoveCollectionItemCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = CollectionItemRemovedEvent(
          collectionItemUuid = collectionItemUuid,
        ),
      )
    }.run(eventService::save)

    assessmentService.fetchLatestState(event.assessment)
      .let { eventBus.handle(event, it) }
      .let { assessmentService.persist(it) }

    return CommandSuccessCommandResult()
  }
}
