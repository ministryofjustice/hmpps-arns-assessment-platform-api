package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Command
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
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
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractCommandHandlerTest {

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

  abstract val handler: KClass<out CommandHandler<out Command>>
  abstract val command: RequestableCommand
  abstract val expectedEvent: Event
  abstract val expectedResult: CommandResult

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  private fun getHandler() = handler.primaryConstructor!!.call(services)

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(getHandler().type).isEqualTo(command::class)
  }

  @Test
  fun `it handles the command`() {
    every { assessmentService.findBy(assessment.uuid) } returns assessment

    val handledEvent = slot<EventEntity<out Event>>()
    val persistedEvent = slot<EventEntity<out Event>>()
    val state: State = mockk()

    every { eventBus.handle(capture(handledEvent)) } returns state
    every { stateService.persist(state) } just Runs
    every { eventService.save(capture(persistedEvent)) } answers { firstArg() }
    every { userDetailsService.findOrCreate(commandUser) } returns user

    val result = getHandler().execute(command)

    verify(exactly = 1) { assessmentService.findBy(assessment.uuid) }
    verify(exactly = 1) { eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { stateService.persist(state) }
    verify(exactly = 1) { eventService.save(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { userDetailsService.findOrCreate(commandUser) }

    assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.uuid)
    assertThat(handledEvent.captured.user.userId).isEqualTo(command.user.id)
    assertThat(handledEvent.captured.user.displayName).isEqualTo(command.user.name)
    assertThat(handledEvent.captured.user.authSource).isEqualTo(command.user.authSource)
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)

    assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

    assertThat(result).isEqualTo(expectedResult)
  }
}
