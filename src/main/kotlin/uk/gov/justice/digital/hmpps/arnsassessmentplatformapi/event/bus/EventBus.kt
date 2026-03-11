package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.StateCollection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.NoStateFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import kotlin.reflect.full.createInstance

class EventBus(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
) {
  private val state: StateCollection = mutableMapOf()

  fun persistState() {
    stateService.persist(state)
    state.clear()
  }

  private fun <E : Event> execute(event: EventEntity<E>) {
    registry.getHandlersFor(event.data::class).forEach { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val stateForAssessment: State = state[event.assessment.uuid] ?: mutableMapOf()
      val stateProvider = stateService.stateForType(aggregateType)
      val stateForType = stateForAssessment[aggregateType]
        ?: stateProvider.fetchLatestStateBefore(event.assessment, event.createdAt)
      if (stateForType == null) {
        stateForAssessment[aggregateType] = stateProvider.blankState(event.assessment)
        state[event.assessment.uuid] = stateForAssessment
        eventService
          .findAllForPointInTime(event.assessment.uuid, event.createdAt)
          .plus(event)
          .sortedBy { it.id }
          .forEach { execute(it) }
      } else {
        stateForAssessment[aggregateType] = handler.handle(event, stateForType)
        state[event.assessment.uuid] = stateForAssessment
      }
    }

    event.children
      .sortedBy { it.createdAt }
      .forEach { execute(it) }
  }

  fun handle(event: EventEntity<*>): State = handle(listOf(event))[event.assessment.uuid]
    ?: throw NoStateFoundException("No state found for assessment ${event.assessment.uuid}")

  fun handle(events: List<EventEntity<*>>): StateCollection = events.forEach { execute(it) }.let { state }
}
