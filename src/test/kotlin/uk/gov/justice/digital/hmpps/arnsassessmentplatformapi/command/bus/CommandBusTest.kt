package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.TestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.CommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AuditService
import kotlin.test.assertEquals

class CommandBusTest {
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
