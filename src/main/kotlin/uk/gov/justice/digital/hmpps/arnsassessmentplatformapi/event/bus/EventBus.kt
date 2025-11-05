package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import kotlin.reflect.full.createInstance

@Component
class EventBus(
  val registry: EventHandlerRegistry,
  val aggregateService: AggregateService,
) {
  // TODO: return in-memory state
  fun <E: Event> handle(event: EventEntity<E>) {
    registry.getHandlersFor(event.data::class).map { handler ->
      val aggregateType = handler.stateType.createInstance().type
      val state = aggregateService.State(aggregateType).fetchLatestState(event.assessment)
      // TODO: fetch in-memory state?
      handler.handle(event, state)
    }
  }
}
