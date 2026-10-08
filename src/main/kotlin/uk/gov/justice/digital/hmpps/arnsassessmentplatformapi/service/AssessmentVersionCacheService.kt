package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class AssessmentVersionCacheService(
  private val redisTemplate: StringRedisTemplate,
  private val objectMapper: ObjectMapper,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun cacheLatest(result: AssessmentVersionQueryResult) {
    runAsync {
      try {
        val payload = objectMapper.writeValueAsString(result)
        redisTemplate.opsForValue().set(cacheKey(result.assessmentUuid), payload, CACHE_TTL)
      } catch (error: Exception) {
        log.error("Failed to cache latest assessment {}", result.assessmentUuid, error)
      }
    }
  }

  fun evictLatestAfterCommit(assessmentUuid: UUID) {
    scheduleAfterCommit {
      runAsync {
        try {
          redisTemplate.delete(cacheKey(assessmentUuid))
        } catch (error: Exception) {
          log.error("Failed to evict cached latest assessment {}", assessmentUuid, error)
        }
      }
    }
  }

  private fun scheduleAfterCommit(action: () -> Unit) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action()

      return
    }

    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun afterCommit() {
          action()
        }
      },
    )
  }

  private fun runAsync(action: () -> Unit) {
    CompletableFuture.runAsync(action)
  }

  companion object {
    val CACHE_TTL: Duration = Duration.ofMinutes(10)

    fun cacheKey(assessmentUuid: UUID) = "assessment:$assessmentUuid:latest"
  }
}
