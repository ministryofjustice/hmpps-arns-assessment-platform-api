package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.CreateAssessmentCommandHandler
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.handlers.result.CreateAssessmentResult

class CommandBusTest {
  val auditService: AuditService = mockk()
  val createAssessmentCommandHandler: CreateAssessmentCommandHandler = mockk()

  @BeforeEach
  fun setUp() {
    every { auditService.audit(any()) } just runs
    every { createAssessmentCommandHandler.execute(any()) } answers { CreateAssessmentResult(firstArg<CreateAssessment>().assessmentUuid) }
    every { createAssessmentCommandHandler.type } returns CreateAssessment::class
  }

  @Test
  fun `it throws when a handler is not implemented for a given command`() {
    val commandBus = CommandBus(
      registry = CommandHandlerRegistry(
        handlers = emptyList(),
      ),
      auditService = auditService,
    )

    val command = CreateAssessment(
      user = User("FOO_USER", "Foo User"),
    )

    assertThrows<HandlerNotImplementedException> {
      commandBus.dispatch(command)
    }
  }

  @Test
  fun `calls the handler for a given command`() {
    val commandBus = CommandBus(
      registry = CommandHandlerRegistry(
        handlers = listOf(createAssessmentCommandHandler),
      ),
      auditService = auditService,
    )

    val command = CreateAssessment(
      user = User("FOO_USER", "Foo User"),
    )

    val result = commandBus.dispatch(command)

    verify(exactly = 1) { createAssessmentCommandHandler.execute(command) }
    verify(exactly = 1) { auditService.audit(command) }

    assert(result is CreateAssessmentResult)

    assertThat((result as CreateAssessmentResult).assessmentUuid).isEqualTo(command.assessmentUuid)
  }
}
