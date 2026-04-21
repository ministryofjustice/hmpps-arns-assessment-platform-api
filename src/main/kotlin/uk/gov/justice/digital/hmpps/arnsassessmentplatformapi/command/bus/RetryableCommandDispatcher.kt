package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command

@Service
class RetryableCommandDispatcher(
  private val transactionalCommandDispatcher: TransactionalCommandDispatcher,
) {
  @Retryable(
    value = [ObjectOptimisticLockingFailureException::class],
    maxAttempts = 3,
    backoff = Backoff(50),
  )
  fun dispatch(commands: List<Command>) = transactionalCommandDispatcher.dispatch(commands)
}
