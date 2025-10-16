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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AggregateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService

class EventBusTest {
  val aggregateService: AggregateService = mockk()
  val eventService: EventService = mockk()

  val user = User("FOO_USER", "Foo User")
  val assessment = AssessmentEntity()

  val aggregate = mockk<AssessmentVersionAggregate>()
  val aggregateType = mockk<AggregateType>()
  val aggregateName = "TEST_AGGREGATE"

  val event = EventEntity(
    user = user,
    assessment = assessment,
    data = AssessmentCreated(),
  )

  @BeforeEach
  fun setUp() {
    every { aggregate.type() } returns aggregateName
    every { aggregateType.aggregateType } returns aggregateName
    every { aggregateService.getAggregateTypes() } returns setOf(aggregateType)
    every {
      aggregateService.processEvents(
        assessment,
        aggregateName,
        any<List<EventEntity>>(),
      )
    } returns AggregateEntity(
      assessment = assessment,
      data = aggregate,
    )

    every { eventService.saveAll(any()) } just runs
  }

  @Test
  fun `it adds events to the queue`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      aggregateService = aggregateService,
      eventService = eventService,
    )

    val event = EventEntity(
      user = user,
      assessment = assessment,
      data = AssessmentCreated(),
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
      aggregateService = aggregateService,
      eventService = eventService,
    )

    every { aggregateType.createsOn } returns emptySet()
    every { aggregateType.updatesOn } returns emptySet()

    eventBus.add(event)
    eventBus.commit()

    verify(exactly = 0) { aggregateService.processEvents(any(), any(), any()) }
    verify(exactly = 1) { eventService.saveAll(queue) }
    Assertions.assertThat(queue).withFailMessage("should flush the queue").isEmpty()
  }

  @Test
  fun `it will update an aggregate configured for the event`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      aggregateService = aggregateService,
      eventService = eventService,
    )

    every { aggregateType.createsOn } returns emptySet()
    every { aggregateType.updatesOn } returns setOf(AssessmentCreated::class)

    eventBus.add(event)
    eventBus.commit()

    verify(exactly = 1) { aggregateService.processEvents(assessment, aggregateName, listOf(event)) }
    verify(exactly = 1) { eventService.saveAll(queue) }
    Assertions.assertThat(queue).withFailMessage("should flush the queue").isEmpty()
  }

  @Test
  fun `it will create an aggregate configured for the event`() {
    val queue = mutableListOf<EventEntity>()
    val eventBus = EventBus(
      queue = queue,
      aggregateService = aggregateService,
      eventService = eventService,
    )

    every { aggregateType.createsOn } returns setOf(AssessmentCreated::class)
    every { aggregateType.updatesOn } returns emptySet()

    eventBus.add(event)
    eventBus.commit()

    verify(exactly = 1) { aggregateService.processEvents(assessment, aggregateName, listOf(event)) }
    verify(exactly = 1) { eventService.saveAll(queue) }
    Assertions.assertThat(queue).withFailMessage("should flush the queue").isEmpty()
  }
}
