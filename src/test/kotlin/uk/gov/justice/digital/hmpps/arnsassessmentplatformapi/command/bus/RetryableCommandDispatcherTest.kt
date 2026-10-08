package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.ObjectOptimisticLockingFailureException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import kotlin.test.assertEquals

class RetryableCommandDispatcherTest : IntegrationTestBase() {
  @Autowired
  private lateinit var retryableCommandDispatcher: RetryableCommandDispatcher

  @MockkBean
  private lateinit var transactionalCommandDispatcher: TransactionalCommandDispatcher

  private val response: CommandsResponse = mockk()

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Test
  fun `dispatches commands via transactional command dispatcher`() {
    val command1 = TestableCommand()
    val command2 = TestableCommand()
    val commands = listOf(command1, command2)

    every { transactionalCommandDispatcher.dispatch(commands) } returns response

    val result = retryableCommandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { transactionalCommandDispatcher.dispatch(commands) }
  }

  @Test
  fun `retries dispatch on optimistic locking failure`() {
    val command = mockk<RequestableCommand>()
    val commands = listOf(command)

    every { transactionalCommandDispatcher.dispatch(commands) } throws
      ObjectOptimisticLockingFailureException("Test", "id") andThenThrows
      ObjectOptimisticLockingFailureException("Test", "id") andThen
      response

    val result = retryableCommandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 3) { transactionalCommandDispatcher.dispatch(commands) }
  }

  @Test
  fun `fails after max retry attempts`() {
    val command = mockk<RequestableCommand>()
    val commands = listOf(command)

    every { transactionalCommandDispatcher.dispatch(commands) } throws
      ObjectOptimisticLockingFailureException("Test", "id")

    assertThrows<ObjectOptimisticLockingFailureException> {
      retryableCommandDispatcher.dispatch(commands)
    }

    verify(exactly = 3) { transactionalCommandDispatcher.dispatch(commands) }
  }
}
