package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.time.LocalDateTime
import java.util.UUID

class StateServiceTest {
  private val aggregateRepository: AggregateRepository = mockk()
  private val eventService: EventService = mockk()
  private val eventBus: EventBus = mockk()
  private val clock: Clock = mockk()
  private val entityManager: EntityManager = mockk()
  private val cacheService: AssessmentVersionCacheService = mockk()

  private val service = StateService(
    aggregateRepository = aggregateRepository,
    eventService = eventService,
    eventBus = eventBus,
    clock = clock,
    entityManager = entityManager,
    assessmentVersionCacheService = cacheService,
  )

  private val now = LocalDateTime.parse("2026-01-02T12:00:00")
  private val assessmentUuid = UUID.fromString("00000000-0000-0000-0000-000000000123")

  @BeforeEach
  fun setUp() {
    every { aggregateRepository.saveAllAndFlush(any<List<AggregateEntity<*>>>()) } answers { firstArg<List<AggregateEntity<*>>>() }
    every { entityManager.detach(any()) } just Runs
    every { cacheService.evictLatestAfterCommit(any()) } just Runs
  }

  @Test
  fun `evicts latest assessment cache entries after persisting state`() {
    val state = mutableMapOf(
      AssessmentAggregate::class to AssessmentState(
        mutableListOf(outdatedAggregate(), latestAggregate()),
      ),
    ) as State

    service.persist(state)

    assertThat((state[AssessmentAggregate::class] as AssessmentState).aggregates).hasSize(1)
    verify(exactly = 1) { aggregateRepository.saveAllAndFlush(any<List<AggregateEntity<*>>>()) }
    verify(exactly = 1) { cacheService.evictLatestAfterCommit(assessmentUuid) }
    verify(exactly = 1) { entityManager.detach(match<AggregateEntity<*>> { it.id == 1L }) }
  }

  private fun assessment() = AssessmentEntity(
    uuid = assessmentUuid,
    createdAt = now.minusDays(1),
    type = "TEST",
  )

  private fun latestAggregate() = AggregateEntity(
    id = 2L,
    uuid = UUID.fromString("00000000-0000-0000-0000-000000000222"),
    assessment = assessment(),
    data = AssessmentAggregate().apply {
      formVersion = "2"
    },
    updatedAt = now,
    eventsFrom = now.minusHours(1),
    eventsTo = now,
    numberOfEventsApplied = 2,
  )

  private fun outdatedAggregate() = AggregateEntity(
    id = 1L,
    uuid = UUID.fromString("00000000-0000-0000-0000-000000000111"),
    assessment = assessment(),
    data = AssessmentAggregate().apply {
      formVersion = "1"
    },
    updatedAt = now.minusHours(2),
    eventsFrom = now.minusHours(2),
    eventsTo = now.minusHours(1),
    numberOfEventsApplied = 1,
  )
}
