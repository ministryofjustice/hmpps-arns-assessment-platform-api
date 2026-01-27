package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
class CreateCollectionCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
  private val userDetailsService: UserDetailsService,
  private val timelineService: TimelineService,
) : CommandHandler<CreateCollectionCommand> {
  override val type = CreateCollectionCommand::class
  override fun handle(command: CreateCollectionCommand): CreateCollectionCommandResult {
    val event = with(command) {
      EventEntity(
        user = userDetailsService.findOrCreate(user),
        assessment = assessmentService.findBy(assessmentUuid),
        data = CollectionCreatedEvent(collectionUuid, name, parentCollectionItemUuid),
      )
    }

    eventBus.handle(event)
      .run(stateService::persist)

    eventService.save(event)
    timelineService.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "collection" to command.name,
          "collectionUuid" to command.collectionUuid,
        ),
      ),
    )

    return CreateCollectionCommandResult(command.collectionUuid)
  }
}
