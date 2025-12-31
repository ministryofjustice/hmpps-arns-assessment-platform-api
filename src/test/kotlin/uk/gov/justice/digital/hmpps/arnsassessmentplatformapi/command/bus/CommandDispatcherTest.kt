package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.test.assertEquals

class CommandDispatcherTest : IntegrationTestBase() {

  @MockkBean
  private lateinit var commandBus: CommandBus

  @MockkBean
  private lateinit var auditService: AuditService

  @Autowired
  private lateinit var commandDispatcher: CommandDispatcher

  private val response: CommandsResponse = mockk()

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Test
  fun `dispatches commands via command bus`() {
    val command1 = TestableCommand()
    val command2 = TestableCommand()
    val commands = listOf(command1, command2)

    every { commandBus.dispatch(commands) } returns response

    val result = commandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { commandBus.dispatch(commands) }
    verify { auditService wasNot Called }
  }

  @Test
  fun `audits requestable commands`() {
    val requestableCommand = mockk<RequestableCommand>()
    val nonRequestableCommand = TestableCommand()
    val commands = listOf(requestableCommand, nonRequestableCommand)

    every { commandBus.dispatch(commands) } returns response
    every { auditService.audit(requestableCommand) } just Runs

    val result = commandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { commandBus.dispatch(commands) }
    verify(exactly = 1) { auditService.audit(requestableCommand) }
  }

  @Test
  fun `does not audit non-requestable commands`() {
    val command = TestableCommand()
    val commands = listOf(command)

    every { commandBus.dispatch(commands) } returns response

    val result = commandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { commandBus.dispatch(commands) }
    verify { auditService wasNot Called }
  }

  @Test
  fun `retries dispatch on optimistic locking failure`() {
    val command = mockk<RequestableCommand>()
    val commands = listOf(command)

    every { commandBus.dispatch(commands) } throws
      ObjectOptimisticLockingFailureException("Test", "id") andThenThrows
      ObjectOptimisticLockingFailureException("Test", "id") andThen
      response
    every { auditService.audit(command) } just Runs

    val result = commandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 3) { commandBus.dispatch(commands) }
    verify(exactly = 1) { auditService.audit(command) }
  }

  @Test
  fun `fails after max retry attempts`() {
    val command = mockk<RequestableCommand>()
    val commands = listOf(command)

    every { commandBus.dispatch(commands) } throws
      ObjectOptimisticLockingFailureException("Test", "id")
    every { auditService.audit(command) } just Runs

    assertThrows<ObjectOptimisticLockingFailureException> {
      commandDispatcher.dispatch(commands)
    }

    verify(exactly = 3) { commandBus.dispatch(commands) }
    verify(exactly = 0) { auditService.audit(command) }
  }
}
