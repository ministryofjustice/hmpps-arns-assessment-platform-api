package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.CreateAssessmentCommandHandler
import java.util.UUID

class CommandBusTest {
  val eventBus: EventBus = mockk()
  val createAssessmentCommandHandler: CreateAssessmentCommandHandler = mockk()

  @BeforeEach
  fun setUp() {
    every { eventBus.add(any()) } just runs
    every { eventBus.commit() } just runs
    every { createAssessmentCommandHandler.execute(any()) } just runs
    every { createAssessmentCommandHandler.type } returns CreateAssessment::class
  }

  @Test
  fun `it throws when a handler is not implemented for a given command`() {
    val commandBus = CommandBus(
      registry = CommandHandlerRegistry(
        handlers = emptyList(),
      ),
      eventBus = eventBus,
    )

    val command = CreateAssessment(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
    )

    assertThrows<HandlerNotImplementedException> {
      commandBus.dispatch(listOf(command))
    }
  }

  @Test
  fun `calls the handler for a given command`() {
    val commandBus = CommandBus(
      registry = CommandHandlerRegistry(
        handlers = listOf(createAssessmentCommandHandler),
      ),
      eventBus = eventBus,
    )

    val command = CreateAssessment(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
    )

    commandBus.dispatch(listOf(command))

    verify(exactly = 1) { createAssessmentCommandHandler.execute(command) }
  }
}
