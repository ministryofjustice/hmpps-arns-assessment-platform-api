package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.bus

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.TestableHook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.handler.HookHandler

class HookBusTest {
  @Test
  fun `dispatch executes every handler registered for each hook, passing through the command and service bundle`() {
    val hook = TestableHook(param = "test")
    val command: RequestableCommand = mockk()
    val serviceBundle: CommandHandlerServiceBundle = mockk()
    val handler1 = mockk<HookHandler<out Hook>>()
    val handler2 = mockk<HookHandler<out Hook>>()
    every { handler1.execute(hook, command, serviceBundle) } just Runs
    every { handler2.execute(hook, command, serviceBundle) } just Runs

    val registry: HookHandlerRegistry = mockk()
    every { registry.getHandlersFor(TestableHook::class) } returns listOf(handler1, handler2)

    HookBus(registry).dispatch(listOf(hook), command, serviceBundle)

    verifySequence {
      handler1.execute(hook, command, serviceBundle)
      handler2.execute(hook, command, serviceBundle)
    }
  }

  @Test
  fun `dispatch is a no-op when no hooks are supplied`() {
    val registry: HookHandlerRegistry = mockk()

    HookBus(registry).dispatch(emptyList(), mockk(), mockk())

    verify(exactly = 0) { registry.getHandlersFor(any()) }
  }
}
