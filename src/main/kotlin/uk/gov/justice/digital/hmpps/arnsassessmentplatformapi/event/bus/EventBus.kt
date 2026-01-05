package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import kotlin.reflect.full.createInstance

@Component
class EventBus(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
) {
  private fun <E : Event, S : State> execute(event: EventEntity<E>, state: S): S {
    registry.getHandlersFor(event.data::class).map { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val stateProvider = stateService.stateForType(aggregateType)
      val stateForType = state[aggregateType] ?: stateProvider.fetchLatestStateBefore(event.assessment, event.createdAt)
      if (stateForType == null) {
        state[aggregateType] = stateProvider.blankState(event.assessment)
        eventService
          .findAllForPointInTime(event.assessment.uuid, event.createdAt)
          .plus(event)
          .sortedWith(compareBy<EventEntity<*>> { it.createdAt }.then(nullsLast(compareBy { it.id })))
          .fold(state) { acc: State, event -> execute(event, acc) }
      } else {
        state[aggregateType] = handler.handle(event, stateForType)
      }
    }
    event.children.sortedBy { it.createdAt }.fold(state) { acc: State, event -> execute(event, acc) }
    return state
  }

  fun handle(event: EventEntity<*>) = handle(listOf(event))

  fun handle(events: List<EventEntity<*>>) = events.fold(mutableMapOf()) { acc: State, event -> execute(event, acc) }
}
