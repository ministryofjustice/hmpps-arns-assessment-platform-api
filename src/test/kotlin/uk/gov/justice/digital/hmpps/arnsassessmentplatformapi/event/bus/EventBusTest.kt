package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import java.time.LocalDateTime
import kotlin.reflect.KClass

class EventBusTest {
  val stateService = mockk<StateService>()
  val eventService = mockk<EventService>()
  val stateProvider = mockk<StateService.StateForType<AssessmentAggregate>>()
  val state: AggregateState<AssessmentAggregate> = mockk()
  val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Test
  fun `calls the handler for a given event`() {
    val event = EventEntity(
      user = mockk(),
      assessment = assessment,
      createdAt = LocalDateTime.now().minusDays(1),
      data = AssessmentCreatedEvent(
        formVersion = "1",
        properties = emptyMap(),
      ),
    )

    val handler1 = mockk<EventHandler<AssessmentCreatedEvent, AggregateState<out Aggregate<*>>>>()
    val handler2 = mockk<EventHandler<AssessmentCreatedEvent, AggregateState<out Aggregate<*>>>>()

    every { handler1.handle(any<EventEntity<AssessmentCreatedEvent>>(), state) } returns state
    every { handler1.stateType } returns AssessmentState::class

    every { handler2.handle(any<EventEntity<AssessmentCreatedEvent>>(), state) } returns state
    every { handler2.stateType } returns AssessmentState::class

    every { stateProvider.fetchLatestStateBefore(assessment, event.createdAt) } returns state

    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val registry: EventHandlerRegistry = mockk()
    every { registry.getHandlersFor(any<KClass<AssessmentCreatedEvent>>()) } returns listOf(handler1, handler2)

    val eventBus = EventBus(
      stateService = stateService,
      eventService = eventService,
      registry = registry,
    )

    eventBus.handle(event)

    verify(exactly = 1) { registry.getHandlersFor(AssessmentCreatedEvent::class) }
    verify(exactly = 1) { handler1.handle(event, state) }
    verify(exactly = 1) { handler2.handle(event, state) }
    verify(exactly = 1) { stateProvider.fetchLatestStateBefore(assessment, event.createdAt) }
    verify(exactly = 2) { stateService.stateForType(AssessmentAggregate::class) }
  }
}
