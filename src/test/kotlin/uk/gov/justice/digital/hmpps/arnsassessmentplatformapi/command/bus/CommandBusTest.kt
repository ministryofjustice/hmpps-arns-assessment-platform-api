package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.TestableCommandResult
import kotlin.test.assertEquals

class CommandBusTest {
  val handler = mockk<CommandHandler<out RequestableCommand>>()
  val registry: CommandHandlerRegistry = mockk()
  val commandBus = CommandBus(registry)

  @BeforeEach
  fun setup() {
    clearAllMocks()
    every { handler.execute(any()) } answers { TestableCommandResult("result-${firstArg<TestableCommand>().param}") }
    every { registry.getHandlerFor(any()) } returns handler
  }

  @Nested
  inner class DispatchSingle {
    @Test
    fun `dispatch a single command`() {
      val response = commandBus.dispatch(TestableCommand(param = "test-1"))

      assertEquals(1, response.commands.size)
      assertEquals(TestableCommand(param = "test-1"), response.commands[0].request)
      assertEquals(TestableCommandResult("result-test-1"), response.commands[0].result)

      verify(exactly = 1) { registry.getHandlerFor(any()) }
      verify(exactly = 1) { handler.execute(any()) }
    }
  }

  @Nested
  inner class DispatchMultiple {
    @Test
    fun `dispatch a list of commands`() {
      val commands = listOf(
        TestableCommand(param = "test-1"),
        TestableCommand(param = "test-2"),
        TestableCommand(param = "test-3"),
      )

      val response = commandBus.dispatch(commands)

      assertEquals(3, response.commands.size)
      assertEquals(TestableCommand(param = "test-1"), response.commands[0].request)
      assertEquals(TestableCommandResult("result-test-1"), response.commands[0].result)

      assertEquals(TestableCommand(param = "test-2"), response.commands[1].request)
      assertEquals(TestableCommandResult("result-test-2"), response.commands[1].result)

      assertEquals(TestableCommand(param = "test-3"), response.commands[2].request)
      assertEquals(TestableCommandResult("result-test-3"), response.commands[2].result)

      verify(exactly = 3) { registry.getHandlerFor(any()) }
      verify(exactly = 3) { handler.execute(any()) }
    }

    @Test
    fun `dispatch should work with empty list`() {
      val response = commandBus.dispatch(emptyList())

      assertEquals(0, response.commands.size)

      verify(exactly = 0) { registry.getHandlerFor(any()) }
      verify(exactly = 0) { handler.execute(any()) }
    }
  }
}
