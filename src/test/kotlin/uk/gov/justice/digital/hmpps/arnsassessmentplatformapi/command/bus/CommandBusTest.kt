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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableRequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.TestableCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.bus.HookBus
import java.util.UUID
import kotlin.test.assertEquals

class CommandBusTest {
  val handler = mockk<CommandHandler<out RequestableCommand>>()
  val commandHandlerFactory: CommandHandlerFactory = mockk()
  val serviceBundle: CommandHandlerServiceBundle = mockk()
  val hookBus: HookBus = mockk()

  val commandBus = CommandBus(
    commandHandlerFactory,
    serviceBundle,
    hookBus,
  )

  @BeforeEach
  fun setup() {
    clearAllMocks()
    every { handler.execute(any()) } answers { TestableCommandResult("result-${firstArg<TestableCommand>().param}") }
    every { commandHandlerFactory.create(any(), any()) } returns handler
    every { serviceBundle.persistenceContext.persist() } just Runs
    every { hookBus.dispatch(any(), any(), any()) } just Runs
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

      verify(exactly = 3) { commandHandlerFactory.create(any(), any()) }
      verify(exactly = 3) { handler.execute(any()) }
      verify(exactly = 0) { hookBus.dispatch(any(), any(), any()) }
      verify(exactly = 1) { serviceBundle.persistenceContext.persist() }
    }

    @Test
    fun `dispatch should work with empty list`() {
      val response = commandBus.dispatchAndPersist(emptyList())

      assertEquals(0, response.commands.size)

      verify(exactly = 0) { commandHandlerFactory.create(any(), any()) }
      verify(exactly = 0) { handler.execute(any()) }
    }
  }

  @Nested
  inner class DispatchHooks {
    private fun requestableCommand(hooks: List<Hook>?) = TestableRequestableCommand(
      user = UserDetails(id = "user-1", name = "Test User"),
      assessmentUuid = UUID.randomUUID().toReference(),
      hooks = hooks,
    )

    @Test
    fun `dispatches the command's hooks for a requestable command that declares them`() {
      val hooks = listOf(mockk<Hook>())
      val command = requestableCommand(hooks)
      every { handler.execute(command) } returns TestableCommandResult("result")

      commandBus.dispatch(listOf(command))

      verify(exactly = 1) { hookBus.dispatch(hooks, command, serviceBundle) }
    }

    @Test
    fun `does not dispatch hooks for a requestable command with none declared`() {
      val command = requestableCommand(hooks = null)
      every { handler.execute(command) } returns TestableCommandResult("result")

      commandBus.dispatch(listOf(command))

      verify(exactly = 0) { hookBus.dispatch(any(), any(), any()) }
    }
  }
}
