package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlingException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

open class EventBus(
  private val registry: EventHandlerRegistry,
  private val stateService: StateService,
  private val persistenceContext: PersistenceContext,
) {
  fun getState() = this.persistenceContext.state

  private fun resolve(resolvers: List<TimelineResolver>): TimelinesResolver = object : TimelinesResolver {
    override fun createTimeline(custom: Timeline?) {
      resolvers.forEach { resolver -> resolver(custom).run(persistenceContext.timeline::add) }
    }
  }

  private fun getAssessmentStateForType(event: EventEntity<*>, aggregateType: KClass<out Aggregate<*>>): Pair<State, AggregateState<out Aggregate<*>>> {
    val stateForAssessment = persistenceContext.state.getOrPut(event.assessment.uuid) { mutableMapOf() }
    val stateForType = stateForAssessment.getOrPut(aggregateType) {
      stateService.stateForType(aggregateType).fetchOrCreateState(event.assessment, event.createdAt)
    }
    return stateForAssessment to stateForType
  }

  private fun <E : Event> execute(event: EventEntity<E>): TimelinesResolver {
    val timelineResolvers = mutableListOf<TimelineResolver>()
    val assessment = event.assessment
    val assessmentState = persistenceContext.state.getOrPut(assessment.uuid) { mutableMapOf() }

    registry.getHandlersFor(event.data::class).forEach { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val aggregateState = assessmentState.getOrPut(aggregateType) {
        stateService.stateForType(aggregateType).fetchOrCreateState(assessment, event.createdAt)
      }

      val result = try {
        handler.handle(event, aggregateState)
      } catch (ex: Exception) {
        throw EventHandlingException(
          eventUuid = event.uuid,
          eventName = event.data::class.simpleName ?: "Unknown",
          handlerName = handler::class.simpleName ?: "Unknown",
          cause = ex,
        )
      }

      assessmentState[aggregateType] = result.state
      result.timeline?.let(timelineResolvers::add)
    }

    persistenceContext.events.add(event)

    return resolve(timelineResolvers)
  }

  fun handle(event: EventEntity<*>) = execute(event)

  fun handle(events: List<EventEntity<*>>) {
    events.forEach { execute(it) }
  }
}
