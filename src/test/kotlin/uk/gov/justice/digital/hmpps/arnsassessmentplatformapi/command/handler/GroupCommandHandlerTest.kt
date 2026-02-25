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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import java.util.UUID

class GroupCommandHandlerTest {
  val assessment = AssessmentEntity(type = "TEST")
  val assessmentService: AssessmentService = mockk()
  val eventService: EventService = mockk()
  val stateService: StateService = mockk()
  val userDetailsService: UserDetailsService = mockk()
  val timelineService: TimelineService = mockk()
  val eventBus: EventBus = mockk()
  val commandBus: CommandBus = mockk()

  val services = CommandHandlerServiceBundle(
    assessment = assessmentService,
    event = eventService,
    state = stateService,
    userDetails = userDetailsService,
    timeline = timelineService,
    eventBus = eventBus,
    commandBus = commandBus,
  )

  val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)

  val timeline = Timeline(type = "test", data = mapOf("foo" to listOf("bar")))

  val handler = GroupCommandHandler(services)

  val command = GroupCommand(
    user = commandUser,
    assessmentUuid = assessment.uuid.toReference(),
    commands = listOf(
      UpdateAssessmentAnswersCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        added = mapOf(),
        removed = listOf(),
      ),
      UpdateFormVersionCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        version = "1.2",
      ),
    ),
    timeline = timeline,
  )

  val expectedEvent = GroupEvent(2)

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
    every { eventService.setParentEvent(any<EventEntity<GroupEvent>>()) } just Runs
    every { eventService.clearParentEvent() } just Runs
    every { commandBus.dispatch(any<List<RequestableCommand>>()) } answers { commandsResponse }
    every { userDetailsService.findOrCreate(commandUser) } returns user

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
    verify(exactly = 1) { userDetailsService.findOrCreate(commandUser) }

    assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.uuid)
    assertThat(handledEvent.captured.user.userId).isEqualTo(command.user.id)
    assertThat(handledEvent.captured.user.displayName).isEqualTo(command.user.name)
    assertThat(handledEvent.captured.user.authSource).isEqualTo(command.user.authSource)
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)
    assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

    assertThat(result).isEqualTo(GroupCommandResult(commands = commandsResponse.commands))
  }
}
