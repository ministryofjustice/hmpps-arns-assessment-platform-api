package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService

@Component
class UpdateCollectionItemAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
) : CommandHandler<UpdateCollectionItemAnswersCommand> {
  override val type = UpdateCollectionItemAnswersCommand::class
  override fun handle(command: UpdateCollectionItemAnswersCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = user,
        assessment = assessmentService.findByUuid(assessmentUuid),
        data = CollectionItemAnswersUpdatedEvent(
          collectionItemUuid = collectionItemUuid,
          added = added,
          removed = removed,
        ),
      )
    }.run(eventService::save)

    assessmentService.fetchLatestState(event.assessment)
      .let { eventBus.handle(event, it) }
      .let { assessmentService.persist(it) }

    return CommandSuccessCommandResult()
  }
}
