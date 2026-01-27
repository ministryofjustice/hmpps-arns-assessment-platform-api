package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
class AddCollectionItemCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
  private val userDetailsService: UserDetailsService,
  private val timelineService: TimelineService,
) : CommandHandler<AddCollectionItemCommand> {
  override val type = AddCollectionItemCommand::class
  override fun handle(command: AddCollectionItemCommand): AddCollectionItemCommandResult {
    val event = with(command) {
      EventEntity(
        user = userDetailsService.findOrCreate(user),
        assessment = assessmentService.findBy(assessmentUuid),
        data = CollectionItemAddedEvent(
          collectionItemUuid,
          collectionUuid,
          answers,
          properties,
          index,
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
          "index" to (command.index ?: (collection.items.size - 1)),
          "collection" to collection.name,
          "collectionItemUuid" to event.data.collectionItemUuid,
          "collectionUuid" to event.data.collectionUuid,
        ),
      ),
    )

    return AddCollectionItemCommandResult(command.collectionItemUuid)
  }
}
