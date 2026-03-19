package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.StateCollection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import kotlin.reflect.full.createInstance

class EventBus(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
  val timelineService: TimelineService,
) {
  private val state: StateCollection = mutableMapOf()
  private val events: MutableList<EventEntity<*>> = mutableListOf()
  private val timeline: MutableList<TimelineEntity> = mutableListOf()

  fun getState() = state

  fun persistState() {
    stateService.persist(state).also { state.clear() }
    eventService.saveAll(events).also { events.clear() }
    timelineService.saveAll(timeline).also { timeline.clear() }
  }

  private fun resolve(resolvers: List<TimelineResolver>): TimelinesResolver = object : TimelinesResolver {
    override fun with(customTimeline: Timeline?) {
      resolvers.forEach { resolver -> resolver(customTimeline).run(timeline::add) }
    }
  }

  private fun <E : Event> execute(event: EventEntity<E>): TimelinesResolver {
    val timelineResolvers = mutableListOf<TimelineResolver>()

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
        val result = handler.handle(event, stateForType)
        stateForAssessment[aggregateType] = result.state
        result.timeline?.run(timelineResolvers::add)
        state[event.assessment.uuid] = stateForAssessment
      }
    }

    events.add(event)

    return resolve(timelineResolvers)
  }

  fun handle(event: EventEntity<*>) = execute(event)

  fun handle(events: List<EventEntity<*>>) {
    events.forEach { execute(it) }
  }
}
