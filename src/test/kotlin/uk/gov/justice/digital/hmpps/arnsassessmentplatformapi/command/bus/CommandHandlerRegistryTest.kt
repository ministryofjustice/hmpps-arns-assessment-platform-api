package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.HandlerNotImplementedException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import kotlin.reflect.KClass

class CommandHandlerRegistryTest {
  @Test
  fun `should return handler for registered command type`() {
    val commandKClass = mockk<KClass<out Command>>()
    val handler = mockk<CommandHandler<out Command>>()
    every { handler.type } returns commandKClass

    val registry = CommandHandlerRegistry(listOf(handler))

    assertEquals(handler, registry.getHandlerFor(commandKClass))
  }

  @Test
  fun `should throw when no handler is registered for the command`() {
    val registry = CommandHandlerRegistry(emptyList())

    val exception = assertThrows(HandlerNotImplementedException::class.java) {
      registry.getHandlerFor(Command::class)
    }

    assertEquals(exception.message, "Unable to dispatch command")
    assertEquals(exception.developerMessage, "No handler registered for type: Command")
  }

  @Test
  fun `should handle multiple command handlers`() {
    val commandKClass1 = mockk<KClass<out Command>>()
    val handler1 = mockk<CommandHandler<out Command>>()
    every { handler1.type } returns commandKClass1

    val commandKClass2 = mockk<KClass<out RequestableCommand>>()
    val handler2 = mockk<CommandHandler<out RequestableCommand>>()
    every { handler2.type } returns commandKClass2

    val registry = CommandHandlerRegistry(listOf(handler1, handler2))

    assertEquals(handler1, registry.getHandlerFor(commandKClass1))
    assertEquals(handler2, registry.getHandlerFor(commandKClass2))
  }
}
