package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.Aggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import java.time.LocalDateTime
import kotlin.reflect.KClass

class EventBusTest {
  val stateService = mockk<StateService>()
  val eventService = mockk<EventService>()
  val timelineService = mockk<TimelineService>()
  val stateProvider = mockk<StateService.StateForType<AssessmentAggregate>>()
  val initialState: AggregateState<AssessmentAggregate> = mockk()
  val assessment = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.now())

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Test
  fun `calls the handlers for a given event`() {
    val event = EventEntity(
      user = mockk(),
      assessment = assessment,
      createdAt = LocalDateTime.now().minusDays(1),
      data = AssessmentCreatedEvent(
        formVersion = "1",
        properties = emptyMap(),
      ),
    )

    val commandTimeline = Timeline(type = "custom_type", data = mapOf("key" to "value"))

    val handler1 = mockk<EventHandler<AssessmentCreatedEvent, AggregateState<out Aggregate<*>>>>()
    val handler2 = mockk<EventHandler<AssessmentCreatedEvent, AggregateState<out Aggregate<*>>>>()

    val handler1Result = mockk<EventHandlerResult<AggregateState<out Aggregate<*>>>>()
    val handler1Timeline = mockk<TimelineResolver>()
    val handler1TimelineEntity = mockk<TimelineEntity>()
    val handler1AggregatesToPersist = mockk<MutableList<AggregateEntity<AssessmentAggregate>>>()
    val handler1State = AssessmentState(handler1AggregatesToPersist)
    every { handler1Result.state } returns handler1State
    every { handler1Result.timeline } returns handler1Timeline
    every { handler1Timeline(commandTimeline) } returns handler1TimelineEntity
    every { handler1.handle(any<EventEntity<AssessmentCreatedEvent>>(), initialState) } returns handler1Result
    every { handler1.stateType } returns AssessmentState::class

    val handler2Result = mockk<EventHandlerResult<AggregateState<out Aggregate<*>>>>()
    val handler2Timeline = mockk<TimelineResolver>()
    val handler2TimelineEntity = mockk<TimelineEntity>()
    val handler2AggregatesToPersist = mockk<MutableList<AggregateEntity<AssessmentAggregate>>>()
    val handler2State = AssessmentState(handler2AggregatesToPersist)
    every { handler2Result.state } returns handler2State
    every { handler2Result.timeline } returns handler2Timeline
    every { handler2Timeline(commandTimeline) } returns handler2TimelineEntity
    every { handler2.handle(any<EventEntity<AssessmentCreatedEvent>>(), handler1State) } returns handler2Result
    every { handler2.stateType } returns AssessmentState::class

    every { stateProvider.fetchLatestStateBefore(assessment, event.createdAt) } returns initialState

    every { stateService.stateForType(AssessmentAggregate::class) } returns stateProvider

    val registry: EventHandlerRegistry = mockk()
    every { registry.getHandlersFor(any<KClass<AssessmentCreatedEvent>>()) } returns listOf(handler1, handler2)

    val eventBus = EventBus(
      stateService = stateService,
      eventService = eventService,
      registry = registry,
      persistenceContext = mockk(),
    )

    eventBus.handle(event).createTimeline(commandTimeline)

    verify(exactly = 1) { registry.getHandlersFor(AssessmentCreatedEvent::class) }
    verify(exactly = 1) { handler1.handle(event, initialState) }
    verify(exactly = 1) { handler2.handle(event, handler1State) }
    verify(exactly = 1) { stateProvider.fetchLatestStateBefore(assessment, event.createdAt) }
    verify(exactly = 2) { stateService.stateForType(AssessmentAggregate::class) }

    verify(exactly = 0) { stateService.persist(any()) }
    verify(exactly = 0) { eventService.saveAll(any()) }
    verify(exactly = 0) { timelineService.saveAll(any()) }

    every { stateService.persist(mutableMapOf(assessment.uuid to mutableMapOf(AssessmentAggregate::class to handler2State))) } just Runs
    every { eventService.saveAll(listOf(event)) } answers { firstArg() }
    every { timelineService.saveAll(listOf(handler1TimelineEntity, handler2TimelineEntity)) } answers { firstArg() }

    eventBus.persistenceContext.persist()

    verify(exactly = 1) { stateService.persist(any()) }
    verify(exactly = 1) { eventService.saveAll(any()) }
    verify(exactly = 1) { timelineService.saveAll(any()) }
  }
}
