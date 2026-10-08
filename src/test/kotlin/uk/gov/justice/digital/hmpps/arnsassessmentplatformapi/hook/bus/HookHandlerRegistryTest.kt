package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.bus

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.Hook
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.hook.handler.HookHandler
import kotlin.reflect.KClass

class HookHandlerRegistryTest {
  @Test
  fun `should return handlers for registered hook type`() {
    val hookKClass = mockk<KClass<out Hook>>()
    val handler = mockk<HookHandler<out Hook>>()
    every { handler.type } returns hookKClass

    val registry = HookHandlerRegistry(listOf(handler))

    assertEquals(listOf(handler), registry.getHandlersFor(hookKClass))
  }

  @Test
  fun `should return empty list when no handler is registered for the hook`() {
    val registry = HookHandlerRegistry(emptyList())

    assertTrue(registry.getHandlersFor(Hook::class).isEmpty())
  }

  @Test
  fun `should group multiple handlers registered for the same hook type`() {
    val hookKClass = mockk<KClass<out Hook>>()
    val handler1 = mockk<HookHandler<out Hook>>()
    val handler2 = mockk<HookHandler<out Hook>>()
    every { handler1.type } returns hookKClass
    every { handler2.type } returns hookKClass

    val registry = HookHandlerRegistry(listOf(handler1, handler2))

    assertEquals(listOf(handler1, handler2), registry.getHandlersFor(hookKClass))
  }
}
