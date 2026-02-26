package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class CreateCollectionCommandHandler(
  private val services: CommandHandlerServiceBundle,
) : CommandHandler<CreateCollectionCommand> {
  override val type = CreateCollectionCommand::class
  override fun handle(command: CreateCollectionCommand): CreateCollectionCommandResult {
    val event = with(command) {
      EventEntity(
        user = services.userDetails.findOrCreate(user),
        assessment = services.assessment.findBy(assessmentUuid.value),
        data = CollectionCreatedEvent(collectionUuid, name, parentCollectionItemUuid?.value),
        createdAt = services.clock.now(),
      )
    }

    services.eventBus.handle(event)
      .run(services.state::persist)

    services.event.save(event)
    services.timeline.save(
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
