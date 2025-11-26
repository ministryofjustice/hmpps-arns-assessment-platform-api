package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.TestableCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.test.assertEquals

class CommandBusTest {
  @Nested
  inner class DispatchSingle {
    val auditService: AuditService = mockk()
    val commandResult: CommandResult = mockk()

    @BeforeEach
    fun setUp() {
      every { auditService.audit(any<RequestableCommand>()) } just runs
    }

    @Test
    fun `calls the handler for a given command and audits it`() {
      val handler = mockk<CommandHandler<out RequestableCommand>>()
      every { handler.execute(any()) } returns commandResult

      val registry: CommandHandlerRegistry = mockk()
      every { registry.getHandlerFor(any()) } returns handler

      val commandBus = CommandBus(registry, auditService)

      val result = commandBus.dispatch(mockk<RequestableCommand>())

      verify(exactly = 1) { registry.getHandlerFor(any()) }
      verify(exactly = 1) { handler.execute(any()) }
      verify(exactly = 1) { auditService.audit(any<RequestableCommand>()) }

      assertEquals(commandResult, result)
    }

    @Test
    fun `non-requestable command is not audited`() {
      val handler = mockk<CommandHandler<out TestableCommand>>()
      every { handler.execute(any()) } returns commandResult

      val registry: CommandHandlerRegistry = mockk()
      every { registry.getHandlerFor(any()) } returns handler

      val commandBus = CommandBus(registry, auditService)

      val result = commandBus.dispatch(mockk<TestableCommand>())

      verify(exactly = 1) { registry.getHandlerFor(any()) }
      verify(exactly = 1) { handler.execute(any()) }
      verify { auditService wasNot Called }

      assertEquals(commandResult, result)
    }
  }

  @Nested
  inner class DispatchMultiple {
    private val commandBus = spyk(CommandBus(mockk(), mockk()), recordPrivateCalls = true)

    @Test
    fun `dispatch should map each Command into a CommandResult`() {
      // given
      val commands = listOf(
        TestableCommand(param = "test-1"),
        TestableCommand(param = "test-2"),
        TestableCommand(param = "test-3"),
      )

      // mock single-command dispatch
      every { commandBus.dispatch(any<TestableCommand>()) } answers { TestableCommandResult("result-${firstArg<TestableCommand>().param}") }

      // when
      val response = commandBus.dispatch(commands)

      // then: ensure the correct number of responses
      assertEquals(3, response.commands.size)

      // ensure objects are mapped correctly
      assertEquals(TestableCommand(param = "test-1"), response.commands[0].request)
      assertEquals(TestableCommandResult("result-test-1"), response.commands[0].result)

      assertEquals(TestableCommand(param = "test-2"), response.commands[1].request)
      assertEquals(TestableCommandResult("result-test-2"), response.commands[1].result)

      assertEquals(TestableCommand(param = "test-3"), response.commands[2].request)
      assertEquals(TestableCommandResult("result-test-3"), response.commands[2].result)

      // verify the inner dispatch(command) was called once per command
      verify(exactly = 3) { commandBus.dispatch(any<TestableCommand>()) }
    }

    @Test
    fun `dispatch should work with empty list`() {
      // when
      val response = commandBus.dispatch(emptyList())

      // then
      assertEquals(0, response.commands.size)
    }
  }
}
