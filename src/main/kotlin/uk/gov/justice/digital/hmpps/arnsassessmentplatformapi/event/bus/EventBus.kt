package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService

@Component
@RequestScope
class EventBus(
    private val queue: MutableList<EventEntity>,
    private val aggregateService: AggregateService,
    private val eventService: EventService,
) {
  fun add(event: EventEntity) {
    queue.add(event)
  }

  fun commit() {
    val eventTypes = queue.map { it.data::class }
    queue.groupBy { it.assessment }
      .forEach { assessment, events ->
        aggregateService.getAggregateTypes()
          .asSequence()
          .filter { aggregate -> aggregate.createsOn.any(eventTypes::contains) || aggregate.updatesOn.any(eventTypes::contains) }
          .forEach { aggregateService.processEvents(assessment, it.aggregateType, events) }
      }
    eventService.saveAll(queue)
    queue.clear()
  }
}
