package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AggregateState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.StateCollection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBusFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContextFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import java.time.LocalDateTime
import java.util.UUID

class StateServiceTest {
  private val aggregateRepository: AggregateRepository = mockk()
  private val eventService: EventService = mockk()
  private val eventBusFactory: EventBusFactory = mockk()
  private val persistenceContextFactory: PersistenceContextFactory = mockk()
  private val clock: Clock = mockk()
  private val cacheService: AssessmentVersionCacheService = mockk()

  private val service = StateService(
    aggregateRepository = aggregateRepository,
    eventService = eventService,
    clock = clock,
    eventBusFactory = eventBusFactory,
    persistenceContextFactory = persistenceContextFactory,
    assessmentVersionCacheService = cacheService,
  )

  private val now = LocalDateTime.parse("2026-01-02T12:00:00")

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { aggregateRepository.saveAll(any<List<AggregateEntity<*>>>()) } answers { firstArg<List<AggregateEntity<*>>>() }
    every { aggregateRepository.findTopByAssessmentUuidAndDataTypeOrderByPositionDesc(any(), any()) } returns null
    every { cacheService.evictLatestAfterCommit(any()) } just Runs
  }

  @Test
  fun `evicts latest assessment cache entries after persisting state`() {
    val assessment = AssessmentEntity(
      createdAt = now.minusDays(1),
      type = "TEST",
    )

    val aggregatesToPersist = mutableListOf(
      AggregateEntity(
        id = 1L,
        uuid = UUID.fromString("00000000-0000-0000-0000-000000000111"),
        assessment = assessment,
        data = AssessmentAggregate().apply {
          formVersion = "1"
        },
        updatedAt = now.minusHours(2),
        eventsFrom = now.minusHours(2),
        eventsTo = now.minusHours(1),
        numberOfEventsApplied = 50,
      ),
      AggregateEntity(
        id = 2L,
        uuid = UUID.fromString("00000000-0000-0000-0000-000000000222"),
        assessment = assessment,
        data = AssessmentAggregate().apply {
          formVersion = "2"
        },
        updatedAt = now,
        eventsFrom = now.minusHours(1),
        eventsTo = now,
        numberOfEventsApplied = 2,
      ),
    )

    val state: StateCollection = mutableMapOf(
      assessment.uuid to mutableMapOf(AssessmentAggregate::class to AssessmentState(aggregatesToPersist)),
    )

    service.persist(state)

    verify(exactly = 1) { aggregateRepository.saveAll(aggregatesToPersist) }
    verify(exactly = 1) { aggregateRepository.findTopByAssessmentUuidAndDataTypeOrderByPositionDesc(assessment.uuid, AssessmentAggregate::class.simpleName!!) }
    verify(exactly = 1) { cacheService.evictLatestAfterCommit(assessment.uuid) }

    assertThat(aggregatesToPersist[0].position).isEqualTo(0)
    assertThat(aggregatesToPersist[1].position).isEqualTo(1)
  }

  @Nested
  inner class Delete {
    @Test
    fun `should delete aggregates by assessment UUID`() {
      val assessmentUuid = UUID.randomUUID()

      every { aggregateRepository.deleteByAssessmentUuid(assessmentUuid) } just Runs

      service.delete(assessmentUuid)

      verify(exactly = 1) { aggregateRepository.deleteByAssessmentUuid(assessmentUuid) }
    }
  }

  @Nested
  inner class RebuildFromEvents {
    val assessment = AssessmentEntity(createdAt = now.minusDays(1), type = "TEST")
    val user = UserDetailsEntity(userId = "FOO_USER", displayName = "Foo User", authSource = AuthSource.HMPPS_AUTH)

    @Test
    fun `should replay events through the event bus and return rebuilt state`() {
      val events = listOf(
        EventEntity(
          user = user,
          assessment = assessment,
          createdAt = now.minusHours(2),
          data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
          position = 0,
        ),
      )

      val rebuiltAggregate = AggregateEntity(
        updatedAt = now,
        eventsFrom = assessment.createdAt,
        eventsTo = events.last().createdAt,
        assessment = assessment,
        data = AssessmentAggregate().apply {
          formVersion = "1"
        },
      )

      val rebuiltState: StateCollection = mutableMapOf(
        assessment.uuid to mutableMapOf(AssessmentAggregate::class to AssessmentState(rebuiltAggregate)),
      )

      val mockPersistenceContext: PersistenceContext = mockk()
      val mockEventBus: EventBus = mockk()

      every { clock.now() } returns now
      every { eventService.findAllForPointInTime(assessment.uuid, now) } returns events
      every { persistenceContextFactory.create() } returns mockPersistenceContext
      every { mockPersistenceContext.state } returns rebuiltState
      every { eventBusFactory.create(mockPersistenceContext) } returns mockEventBus
      every { mockEventBus.handle(any<List<EventEntity<*>>>()) } just Runs
      every { mockEventBus.getState() } returns rebuiltState

      val result = service.rebuildFromEvents(assessment, null)

      assertStateIsEqualTo(result, rebuiltState[assessment.uuid]!!)

      verify(exactly = 1) { eventService.findAllForPointInTime(assessment.uuid, now) }
      verify(exactly = 1) { mockEventBus.handle(any<List<EventEntity<*>>>()) }
    }

    @Test
    fun `should use the provided point in time instead of now`() {
      val pointInTime = now.minusHours(6)
      val events = listOf(
        EventEntity(
          user = user,
          assessment = assessment,
          createdAt = now.minusHours(8),
          data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
          position = 0,
        ),
      )

      val rebuiltState: StateCollection = mutableMapOf(
        assessment.uuid to mutableMapOf(AssessmentAggregate::class to AssessmentState()),
      )

      val mockPersistenceContext: PersistenceContext = mockk()
      val mockEventBus: EventBus = mockk()

      every { clock.now() } returns now
      every { eventService.findAllForPointInTime(assessment.uuid, pointInTime) } returns events
      every { persistenceContextFactory.create() } returns mockPersistenceContext
      every { mockPersistenceContext.state } returns rebuiltState
      every { eventBusFactory.create(mockPersistenceContext) } returns mockEventBus
      every { mockEventBus.handle(any<List<EventEntity<*>>>()) } just Runs
      every { mockEventBus.getState() } returns rebuiltState

      service.rebuildFromEvents(assessment, pointInTime)

      verify(exactly = 1) { eventService.findAllForPointInTime(assessment.uuid, pointInTime) }
    }

    @Test
    fun `should return an empty aggregate when no events exist`() {
      val rebuiltState: StateCollection = mutableMapOf(
        assessment.uuid to mutableMapOf(AssessmentAggregate::class to AssessmentState()),
      )

      val mockPersistenceContext: PersistenceContext = mockk()
      val mockEventBus: EventBus = mockk()

      every { clock.now() } returns now
      every { eventService.findAllForPointInTime(assessment.uuid, now) } returns emptyList()
      every { persistenceContextFactory.create() } returns mockPersistenceContext
      every { mockPersistenceContext.state } returns rebuiltState
      every { eventBusFactory.create(mockPersistenceContext) } returns mockEventBus
      every { mockEventBus.handle(any<List<EventEntity<*>>>()) } just Runs
      every { mockEventBus.getState() } returns rebuiltState

      val result = service.rebuildFromEvents(assessment, null)

      assertStateIsEqualTo(result, rebuiltState[assessment.uuid]!!)

      verify(exactly = 1) { eventService.findAllForPointInTime(assessment.uuid, now) }
    }
  }

  companion object {
    fun assertStateIsEqualTo(actualState: State, expectedState: State) {
      assertThat(actualState).hasSize(expectedState.size)
      assertThat(actualState).containsOnlyKeys(expectedState.keys)
      actualState.keys.forEach { key ->
        val actualAggregateState = assertInstanceOf<AggregateState<*>>(actualState[key])
        val expectedAggregateState = assertInstanceOf<AggregateState<*>>(expectedState[key])

        assertThat(actualAggregateState.type).isEqualTo(expectedAggregateState.type)
        assertThat(actualAggregateState.aggregates).hasSize(expectedAggregateState.aggregates.size)

        actualAggregateState.aggregates.forEachIndexed { index, actualAggregate ->
          val expectedAggregate = expectedAggregateState.aggregates[index]

          assertThat(actualAggregate.assessment).isEqualTo(expectedAggregate.assessment)
          assertThat(actualAggregate.numberOfEventsApplied).isEqualTo(expectedAggregate.numberOfEventsApplied)
          assertThat(actualAggregate.eventsFrom).isEqualTo(expectedAggregate.eventsFrom)
          assertThat(actualAggregate.eventsTo).isEqualTo(expectedAggregate.eventsTo)
          assertThat(actualAggregate.data).isEqualTo(expectedAggregate.data)
        }
      }
    }
  }
}
