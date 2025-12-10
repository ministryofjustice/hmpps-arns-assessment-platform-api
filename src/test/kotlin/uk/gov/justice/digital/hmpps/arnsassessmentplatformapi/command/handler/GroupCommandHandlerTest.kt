package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.GroupCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.GroupCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

class GroupCommandHandlerTest {
  val assessment = AssessmentEntity()
  val assessmentService: AssessmentService = mockk()
  val eventBus: EventBus = mockk()
  val eventService: EventService = mockk(relaxed = true)
  val stateService: StateService = mockk()
  val commandBus: CommandBus = mockk()

  val user = User("FOO_USER", "Foo User")
  val timeline = Timeline(type = "test", data = mapOf("foo" to listOf("bar")))

  val handler = GroupCommandHandler(
    assessmentService,
    eventBus,
    eventService,
    stateService,
    commandBus,
  )

  val command = GroupCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    commands = listOf(
      UpdateAssessmentAnswersCommand(
        user = user,
        assessmentUuid = assessment.uuid,
        added = mapOf(),
        removed = listOf(),
      ),
      UpdateFormVersionCommand(
        user = user,
        assessmentUuid = assessment.uuid,
        version = "1.2",
      ),
    ),
    timeline = timeline,
  )

  val expectedEvent = GroupEvent(
    timeline = command.timeline,
  )

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(command::class)
  }

  @Test
  fun `it handles the command`() {
    every { assessmentService.findBy(assessment.uuid) } returns assessment

    val handledEvent = slot<EventEntity<*>>()
    val persistedEvent = slot<EventEntity<GroupEvent>>()
    val commandsResponse = CommandsResponse(commands = mockk())
    val state: State = mockk()

    every { eventBus.handle(capture(handledEvent)) } returns state
    every { stateService.persist(state) } just Runs
    every { eventService.save(capture(persistedEvent)) } answers { firstArg() }
    every { commandBus.dispatch(any<List<RequestableCommand>>()) } answers { commandsResponse }

    val result = handler.execute(command)

    verify(exactly = 1) { assessmentService.findBy(assessment.uuid) }
    verify(exactly = 1) { eventBus.handle(any<EventEntity<*>>()) }
    verify(exactly = 1) { stateService.persist(state) }
    verify(exactly = 1) { eventService.save(any<EventEntity<*>>()) }
    verify(exactly = 1) { eventService.setParentEvent(any()) }
    verify(exactly = 1) { eventService.clearParentEvent() }
    verifyOrder {
      eventService.save(persistedEvent.captured)
      eventService.setParentEvent(persistedEvent.captured)
      eventService.clearParentEvent()
    }
    verify(exactly = 1) { commandBus.dispatch(command.commands) }

    assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.uuid)
    assertThat(handledEvent.captured.user).isEqualTo(command.user)
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)
    assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

    assertThat(result).isEqualTo(GroupCommandResult(commands = commandsResponse.commands))
  }
}
