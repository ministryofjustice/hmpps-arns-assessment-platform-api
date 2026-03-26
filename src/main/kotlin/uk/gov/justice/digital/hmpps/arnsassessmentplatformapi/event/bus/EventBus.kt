package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

open class EventBus(
  val registry: EventHandlerRegistry,
  val stateService: StateService,
  val eventService: EventService,
  val persistenceContext: PersistenceContext,
) {
  private fun resolve(resolvers: List<TimelineResolver>): TimelinesResolver = object : TimelinesResolver {
    override fun createTimeline(custom: Timeline?) {
      resolvers.forEach { resolver -> resolver(custom).run(persistenceContext.timeline::add) }
    }
  }

  private fun getAssessmentStateForType(event: EventEntity<*>, aggregateType: KClass<out Aggregate<*>>): Pair<State, AggregateState<out Aggregate<*>>> {
    val stateProvider = stateService.stateForType(aggregateType)
    val stateForAssessment: State = persistenceContext.state[event.assessment.uuid] ?: mutableMapOf()
    val stateForType = stateForAssessment[aggregateType]
      ?: stateProvider.fetchLatestStateBefore(event.assessment, event.createdAt)

    if (stateForType == null) {
      stateForAssessment[aggregateType] = stateProvider.blankState(event.assessment)
      persistenceContext.state[event.assessment.uuid] = stateForAssessment
      eventService
        .findAllForPointInTime(event.assessment.uuid, event.createdAt)
        .sortedBy { it.id }
        .forEach { execute(it) }

      return getAssessmentStateForType(event, aggregateType)
    }

    return Pair(stateForAssessment, stateForType)
  }

  private fun <E : Event> execute(event: EventEntity<E>): TimelinesResolver {
    val timelineResolvers = mutableListOf<TimelineResolver>()

    registry.getHandlersFor(event.data::class).forEach { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val (stateForAssessment, stateForType) = getAssessmentStateForType(event, aggregateType)
      val result = handler.handle(event, stateForType)
      stateForAssessment[aggregateType] = result.state
      result.timeline?.run(timelineResolvers::add)
      persistenceContext.state[event.assessment.uuid] = stateForAssessment
    }

    persistenceContext.events.add(event)

    return resolve(timelineResolvers)
  }

  fun handle(event: EventEntity<*>) = execute(event)

  fun handle(events: List<EventEntity<*>>) {
    events.forEach { execute(it) }
  }
}
