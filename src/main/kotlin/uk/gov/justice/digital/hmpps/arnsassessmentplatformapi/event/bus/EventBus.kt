package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.StateCollection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventProto
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

open class EventBus(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
  val timelineService: TimelineService,
) {
  private val state: StateCollection = mutableMapOf()
  private val events: MutableList<EventProto<*>> = mutableListOf()
  private val timeline: MutableList<TimelineEntity> = mutableListOf()

  fun getState() = state

  @Transactional
  open fun persistState() {
    eventService.saveAll(events).also { events.clear() }
    timelineService.saveAll(timeline).also { timeline.clear() }
    stateService.persist(state).also { state.clear() }
  }

  private fun resolve(resolvers: List<TimelineResolver>): TimelinesResolver = object : TimelinesResolver {
    override fun createTimeline(custom: Timeline?) {
      resolvers.forEach { resolver -> resolver(custom).run(timeline::add) }
    }
  }

  private fun getAssessmentStateForType(event: EventProto<*>, aggregateType: KClass<out Aggregate<*>>): Pair<State, AggregateState<out Aggregate<*>>> {
    val stateProvider = stateService.stateForType(aggregateType)
    val stateForAssessment: State = state[event.assessment.uuid] ?: mutableMapOf()
    val stateForType = stateForAssessment[aggregateType]
      ?: stateProvider.fetchLatestStateBefore(event.assessment, event.createdAt)

    if (stateForType == null) {
      stateForAssessment[aggregateType] = stateProvider.blankState(event.assessment)
      state[event.assessment.uuid] = stateForAssessment
      eventService
        .findAllForPointInTime(event.assessment.uuid, event.createdAt)
        .sortedBy { it.id }
        .map { EventProto.from(it) }
        .forEach { execute(it) }

      return getAssessmentStateForType(event, aggregateType)
    }

    return Pair(stateForAssessment, stateForType)
  }

  private fun <E : Event> execute(event: EventProto<E>): TimelinesResolver {
    val timelineResolvers = mutableListOf<TimelineResolver>()

    registry.getHandlersFor(event.data::class).forEach { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val (stateForAssessment, stateForType) = getAssessmentStateForType(event, aggregateType)
      val result = handler.handle(event, stateForType)
      stateForAssessment[aggregateType] = result.state
      result.timeline?.run(timelineResolvers::add)
      state[event.assessment.uuid] = stateForAssessment
    }

    events.add(event)

    return resolve(timelineResolvers)
  }

  fun handle(event: EventProto<*>) = execute(event)

  fun handle(events: List<EventProto<*>>) {
    events.forEach { execute(it) }
  }
}
