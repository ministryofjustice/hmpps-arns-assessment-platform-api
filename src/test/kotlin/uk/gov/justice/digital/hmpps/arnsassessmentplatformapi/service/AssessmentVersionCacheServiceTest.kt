package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class AssessmentVersionCacheServiceTest {
  private val redisTemplate: StringRedisTemplate = mockk()
  private val valueOperations: ValueOperations<String, String> = mockk()
  private val objectMapper: ObjectMapper = mockk()
  private val service = AssessmentVersionCacheService(redisTemplate, objectMapper)

  private val assessmentUuid = UUID.randomUUID()
  private val result = AssessmentVersionQueryResult(
    assessmentUuid = assessmentUuid,
    aggregateUuid = UUID.randomUUID(),
    assessmentType = "TEST",
    formVersion = "1",
    createdAt = LocalDateTime.parse("2026-01-02T12:00:00"),
    updatedAt = LocalDateTime.parse("2026-01-02T12:05:00"),
    answers = emptyMap(),
    properties = emptyMap(),
    collections = emptyList(),
    collaborators = emptySet(),
    identifiers = emptyMap(),
    assignedUser = null,
  )

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { redisTemplate.opsForValue() } returns valueOperations
    every { valueOperations.set(any(), any(), any<Duration>()) } just Runs
    every { redisTemplate.delete(any<String>()) } returns true
    every { objectMapper.writeValueAsString(result) } returns "{\"cached\":true}"
  }

  @Test
  fun `writes the latest assessment to cache`() {
    service.cacheLatest(result)

    verify(timeout = 1000, exactly = 1) { objectMapper.writeValueAsString(result) }
    verify(timeout = 1000, exactly = 1) {
      valueOperations.set(
        AssessmentVersionCacheService.cacheKey(assessmentUuid),
        "{\"cached\":true}",
        AssessmentVersionCacheService.CACHE_TTL,
      )
    }
  }

  @Test
  fun `evicts the latest assessment after commit`() {
    TransactionSynchronizationManager.initSynchronization()

    service.evictLatestAfterCommit(assessmentUuid)

    verify(exactly = 0) { redisTemplate.delete(any<String>()) }

    TransactionSynchronizationManager.getSynchronizations()
      .single()
      .afterCommit()

    verify(timeout = 1000, exactly = 1) { redisTemplate.delete(AssessmentVersionCacheService.cacheKey(assessmentUuid)) }
  }

  @Test
  fun `does not evict the latest assessment on rollback`() {
    TransactionSynchronizationManager.initSynchronization()

    service.evictLatestAfterCommit(assessmentUuid)

    TransactionSynchronizationManager.getSynchronizations()
      .single()
      .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)

    Thread.sleep(50)

    verify(exactly = 0) { redisTemplate.delete(any<String>()) }
  }

  @AfterEach
  fun tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization()
    }
  }
}
