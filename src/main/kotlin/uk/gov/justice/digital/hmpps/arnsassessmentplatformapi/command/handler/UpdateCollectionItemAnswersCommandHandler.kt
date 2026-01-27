package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
class UpdateCollectionItemAnswersCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
  private val userDetailsService: UserDetailsService,
  private val timelineService: TimelineService,
) : CommandHandler<UpdateCollectionItemAnswersCommand> {
  override val type = UpdateCollectionItemAnswersCommand::class
  override fun handle(command: UpdateCollectionItemAnswersCommand): CommandSuccessCommandResult {
    val event = with(command) {
      EventEntity(
        user = userDetailsService.findOrCreate(user),
        assessment = assessmentService.findBy(assessmentUuid),
        data = CollectionItemAnswersUpdatedEvent(
          collectionItemUuid = collectionItemUuid,
          added = added,
          removed = removed,
        ),
      )
    }

    val collection = eventBus.handle(event)
      .also { updatedState -> stateService.persist(updatedState) }
      .run { get(AssessmentAggregate::class) as AssessmentState }
      .getForRead().data.getCollection(command.collectionItemUuid)
      ?: throw Error("Collection $command.collectionItemUuid not found")

    eventService.save(event)
    timelineService.save(
      TimelineEntity.from(
        command,
        event,
        mapOf(
          "collection" to collection.name,
          "index" to collection.findItem(command.collectionItemUuid).run(collection.items::indexOf),
          "added" to command.added.keys,
          "removed" to command.removed,
        ),
      ),
    )

    return CommandSuccessCommandResult()
  }
}
