package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundleFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.TestableCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import kotlin.test.assertEquals

class CommandBusTest {
  val handler = mockk<CommandHandler<out RequestableCommand>>()
  val eventBus: EventBus = mockk()
  val commandHandlerFactory: CommandHandlerFactory = mockk()
  val serviceBundleFactory: CommandHandlerServiceBundleFactory = mockk()

  val commandBus = CommandBus(
    eventBus,
    commandHandlerFactory,
    serviceBundleFactory,
  )

  @BeforeEach
  fun setup() {
    clearAllMocks()
    every { handler.execute(any()) } answers { TestableCommandResult("result-${firstArg<TestableCommand>().param}") }
    every { serviceBundleFactory.create(any()) } returns mockk()
    every { commandHandlerFactory.create(any(), any()) } returns handler
    every { eventBus.persistState() } just Runs
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

      val response = commandBus.dispatchAndPersist(commands)

      assertEquals(3, response.commands.size)
      assertEquals(TestableCommand(param = "test-1"), response.commands[0].request)
      assertEquals(TestableCommandResult("result-test-1"), response.commands[0].result)

      assertEquals(TestableCommand(param = "test-2"), response.commands[1].request)
      assertEquals(TestableCommandResult("result-test-2"), response.commands[1].result)

      assertEquals(TestableCommand(param = "test-3"), response.commands[2].request)
      assertEquals(TestableCommandResult("result-test-3"), response.commands[2].result)

      verify(exactly = 3) { serviceBundleFactory.create(any()) }
      verify(exactly = 3) { commandHandlerFactory.create(any(), any()) }
      verify(exactly = 3) { handler.execute(any()) }
      verify(exactly = 1) { eventBus.persistState() }
    }

    @Test
    fun `dispatch should work with empty list`() {
      val response = commandBus.dispatchAndPersist(emptyList())

      assertEquals(0, response.commands.size)

      verify(exactly = 0) { serviceBundleFactory.create(any()) }
      verify(exactly = 0) { commandHandlerFactory.create(any(), any()) }
      verify(exactly = 0) { handler.execute(any()) }
    }
  }
}
