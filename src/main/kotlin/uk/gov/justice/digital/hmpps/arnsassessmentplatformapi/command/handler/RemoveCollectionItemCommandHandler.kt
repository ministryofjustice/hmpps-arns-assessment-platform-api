package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService

@Component
class RemoveCollectionItemCommandHandler(
  private val assessmentService: AssessmentService,
  private val eventBus: EventBus,
  private val eventService: EventService,
  private val stateService: StateService,
  private val userDetailsService: UserDetailsService,
) : CommandHandler<RemoveCollectionItemCommand> {
  override val type = RemoveCollectionItemCommand::class
  override fun handle(command: RemoveCollectionItemCommand): CommandSuccessCommandResult {
    val assessment = assessmentService.findBy(command.assessmentUuid)
    val state: AssessmentState = stateService
      .stateForType(AssessmentAggregate::class)
      .fetchOrCreateLatestState(assessment)
    val collection = state.getForRead().data.getCollection(command.collectionItemUuid)
      ?: throw Error("Collection ${command.collectionItemUuid} not found")
    val itemIndex = collection.items.size
    val collectionName = collection.name

    val event = with(command) {
      EventEntity(
        user = userDetailsService.findOrCreate(user),
        assessment = assessmentService.findBy(assessmentUuid),
        data = CollectionItemRemovedEvent(
          collectionName = collectionName,
          collectionItemUuid = collectionItemUuid,
          index = itemIndex,
          timeline = timeline,
        ),
      )
    }

    eventBus.handle(event).run(stateService::persist)
    eventService.save(event)

    return CommandSuccessCommandResult()
  }
}
