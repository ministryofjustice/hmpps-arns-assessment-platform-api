package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateTypeRegistry
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

class EventBusTest {
  val stateService: StateService = mockk()
  val eventService: EventService = mockk()
  val aggregateTypeRegistry: AggregateTypeRegistry = mockk()

  val user = User("FOO_USER", "Foo User")
  val assessment = AssessmentEntity()

  val aggregate = mockk<AssessmentVersionAggregate>()
  val aggregateType = mockk<AggregateType>()
  val aggregateName = AssessmentVersionAggregate::class

  val event = EventEntity(
    user = user,
    assessment = assessment,
    data = AssessmentCreatedEvent(),
  )

  @BeforeEach
  fun setUp() {
    every {
      stateService.processEvents(
        assessment,
        aggregateName,
        any<List<EventEntity>>(),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = aggregate,
    )

    every { eventService.saveAll(any()) } just runs
    every { aggregateTypeRegistry.getAggregates() } returns mapOf(
      AssessmentVersionAggregate::class to aggregateType,
    )
  }

  @Test
  fun `it adds events to the queue`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      stateService = stateService,
      eventService = eventService,
      aggregateTypeRegistry = aggregateTypeRegistry,
    )

    val event = EventEntity(
      user = user,
      assessment = assessment,
      data = AssessmentCreatedEvent(),
    )

    eventBus.add(event)

    Assertions.assertThat(queue.size).isEqualTo(1)
    Assertions.assertThat(queue).contains(event)
  }

  @Test
  fun `it commits events in the queue`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      stateService = stateService,
      eventService = eventService,
      aggregateTypeRegistry = aggregateTypeRegistry,
    )

    every { aggregateType.createsOn } returns emptySet()
    every { aggregateType.updatesOn } returns emptySet()

    eventBus.add(event)
    eventBus.commit()

    verify(exactly = 0) { stateService.processEvents(any(), any(), any()) }
    verify(exactly = 1) { eventService.saveAll(queue) }
    Assertions.assertThat(queue).withFailMessage("should flush the queue").isEmpty()
  }

  @Test
  fun `it will update an aggregate configured for the event`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      stateService = stateService,
      eventService = eventService,
      aggregateTypeRegistry = aggregateTypeRegistry,
    )

    every { aggregateType.createsOn } returns emptySet()
    every { aggregateType.updatesOn } returns setOf(AssessmentCreatedEvent::class)

    eventBus.add(event)
    eventBus.commit()

    verify(exactly = 1) { stateService.processEvents(assessment, aggregateName, listOf(event)) }
    verify(exactly = 1) { eventService.saveAll(queue) }
    Assertions.assertThat(queue).withFailMessage("should flush the queue").isEmpty()
  }

  @Test
  fun `it will create an aggregate configured for the event`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      stateService = stateService,
      eventService = eventService,
      aggregateTypeRegistry = aggregateTypeRegistry,
    )

    every { aggregateType.createsOn } returns setOf(AssessmentCreatedEvent::class)
    every { aggregateType.updatesOn } returns emptySet()

    eventBus.add(event)
    eventBus.commit()

    verify(exactly = 1) { stateService.processEvents(assessment, aggregateName, listOf(event)) }
    verify(exactly = 1) { eventService.saveAll(queue) }
    Assertions.assertThat(queue).withFailMessage("should flush the queue").isEmpty()
  }
}
