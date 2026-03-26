package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBusFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.time.LocalDateTime
import java.util.UUID

class StateServiceTest {
  private val aggregateRepository: AggregateRepository = mockk()
  private val eventService: EventService = mockk()
  private val eventBusFactory: EventBusFactory = mockk()
  private val clock: Clock = mockk()
  private val cacheService: AssessmentVersionCacheService = mockk()

  private val service = StateService(
    aggregateRepository = aggregateRepository,
    eventService = eventService,
    clock = clock,
    eventBusFactory = eventBusFactory,
    assessmentVersionCacheService = cacheService,
  )

  private val now = LocalDateTime.parse("2026-01-02T12:00:00")

  @BeforeEach
  fun setUp() {
    every { aggregateRepository.saveAll(any<List<AggregateEntity<*>>>()) } answers { firstArg<List<AggregateEntity<*>>>() }
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

    val state = mutableMapOf(
      assessment.uuid to mutableMapOf(AssessmentAggregate::class to AssessmentState(aggregatesToPersist)) as State,
    )

    service.persist(state)

    verify(exactly = 1) { aggregateRepository.saveAll(aggregatesToPersist) }
    verify(exactly = 1) { cacheService.evictLatestAfterCommit(assessment.uuid) }
  }
}
