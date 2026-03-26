package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentEventHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventHandlerResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventProto
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity

@Component
class CollectionItemRemovedEventHandler(
  private val clock: Clock,
) : AssessmentEventHandler<CollectionItemRemovedEvent> {
  override val eventType = CollectionItemRemovedEvent::class
  override val stateType = AssessmentState::class

  override fun handle(
    event: EventProto<CollectionItemRemovedEvent>,
    state: AssessmentState,
  ): EventHandlerResult<AssessmentState> {
    val aggregate = state.getForWrite(clock)

    val collection = aggregate.data.getCollectionWithItem(event.data.collectionItemUuid)
      ?: throw CollectionItemNotFoundException(event.data.collectionItemUuid, aggregate.uuid)

    val timelineData = mapOf(
      "collection" to collection.name,
      "index" to collection.items.indexOf(collection.findItem(event.data.collectionItemUuid)),
    )

    collection.removeItem(event.data.collectionItemUuid)

    aggregate.data.apply {
      collaborators.add(event.user.uuid)
    }

    aggregate.apply {
      eventsTo = event.createdAt
      updatedAt = clock.now()
      numberOfEventsApplied += 1
    }

    return EventHandlerResult(
      state = state,
      timeline = TimelineEntity.resolver(event, timelineData),
    )
  }
}
