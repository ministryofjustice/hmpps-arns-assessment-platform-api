package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.exception.EventHandlingException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver
import kotlin.collections.set
import kotlin.reflect.full.createInstance

open class EventBus(
  private val registry: EventHandlerRegistry,
  private val persistenceContext: PersistenceContext,
) {
  fun getState() = this.persistenceContext.state

  private fun resolve(resolvers: List<TimelineResolver>): TimelinesResolver = object : TimelinesResolver {
    override fun createTimeline(custom: Timeline?) {
      resolvers.forEach { resolver -> resolver(custom).run(persistenceContext.timeline::add) }
    }
  }

  private fun <E : Event> execute(event: EventEntity<E>): TimelinesResolver {
    val timelineResolvers = mutableListOf<TimelineResolver>()
    val assessment = event.assessment

    registry.getHandlersFor(event.data::class).forEach { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val aggregateState = persistenceContext.getAggregateState(assessment, aggregateType, event.createdAt)

      val result = runCatching {
        handler.handle(event, aggregateState)
      }.getOrElse {
        throw EventHandlingException(
          eventUuid = event.uuid,
          eventName = event.data::class.simpleName ?: "Unknown",
          handlerName = handler::class.simpleName ?: "Unknown",
          cause = it,
        )
      }

      persistenceContext.setAggregateState(assessment.uuid, aggregateType, result.state)
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
