package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component
import org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity

@Component
@Scope(value = SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
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
          .filter { it.createsOn.any(eventTypes::contains) || it.updatesOn.any(eventTypes::contains) }
          .forEach { aggregateService.processEvents(assessment, it.aggregateType, events) }
      }
    eventService.saveAll(queue)
  }
}
