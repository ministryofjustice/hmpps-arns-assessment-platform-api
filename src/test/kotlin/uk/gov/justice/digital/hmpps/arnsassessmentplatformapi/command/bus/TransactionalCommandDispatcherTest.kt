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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.test.assertEquals

class TransactionalCommandDispatcherTest : IntegrationTestBase() {
  val commandBus: CommandBus = mockk()

  @MockkBean
  private lateinit var commandBusFactory: CommandBusFactory

  @MockkBean
  private lateinit var auditService: AuditService

  @Autowired
  private lateinit var transactionalCommandDispatcher: TransactionalCommandDispatcher

  private val response: CommandsResponse = mockk()

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { commandBusFactory.create() } returns commandBus
  }

  @Test
  fun `dispatches commands via command bus`() {
    val command1 = TestableCommand()
    val command2 = TestableCommand()
    val commands = listOf(command1, command2)

    every { commandBus.dispatchAndPersist(commands) } returns response

    val result = transactionalCommandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { commandBus.dispatchAndPersist(commands) }
    verify { auditService wasNot Called }
  }

  @Test
  fun `audits requestable commands`() {
    val requestableCommand = mockk<RequestableCommand>()
    val nonRequestableCommand = TestableCommand()
    val commands = listOf(requestableCommand, nonRequestableCommand)

    every { commandBus.dispatchAndPersist(commands) } returns response
    every { auditService.audit(listOf(requestableCommand)) } just Runs

    val result = transactionalCommandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { commandBus.dispatchAndPersist(commands) }
    verify(exactly = 1) { auditService.audit(listOf(requestableCommand)) }
  }

  @Test
  fun `does not audit non-requestable commands`() {
    val command = TestableCommand()
    val commands = listOf(command)

    every { commandBus.dispatchAndPersist(commands) } returns response

    val result = transactionalCommandDispatcher.dispatch(commands)

    assertEquals(response, result)

    verify(exactly = 1) { commandBus.dispatchAndPersist(commands) }
    verify { auditService wasNot Called }
  }

  @Test
  fun `does not audit on failure`() {
    val requestableCommand = mockk<RequestableCommand>()
    val commands = listOf(requestableCommand)

    every { commandBus.dispatchAndPersist(commands) } throws RuntimeException()

    assertThrows<RuntimeException> {
      transactionalCommandDispatcher.dispatch(commands)
    }

    verify(exactly = 1) { commandBus.dispatchAndPersist(commands) }
    verify { auditService wasNot Called }
  }
}
