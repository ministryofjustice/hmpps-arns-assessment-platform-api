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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class AbstractCommandHandlerTest {
  val assessment = AssessmentEntity()
  val assessmentService: AssessmentService = mockk()
  val eventBus: EventBus = mockk()
  val eventService: EventService = mockk()
  val stateService: StateService = mockk()

  val user = User("FOO_USER", "Foo User")
  val timeline = Timeline(type = "test", data = mapOf("foo" to listOf("bar")))

  abstract val handler: KClass<out CommandHandler<out Command>>
  abstract val command: RequestableCommand
  abstract val expectedEvent: Event
  abstract val expectedResult: CommandResult

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  private fun getHandler() = handler.primaryConstructor!!.call(
    assessmentService,
    eventBus,
    eventService,
    stateService,
  )

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(getHandler().type).isEqualTo(command::class)
  }

  @Test
  fun `it handles the command`() {
    every { assessmentService.findByUuid(assessment.uuid) } returns assessment

    val handledEvent = slot<EventEntity<out Event>>()
    val persistedEvent = slot<EventEntity<out Event>>()
    val state: State = mockk()

    every { eventBus.handle(capture(handledEvent)) } returns state
    every { stateService.persist(state) } just Runs
    every { eventService.save(capture(persistedEvent)) } answers { firstArg() }

    val result = getHandler().execute(command)

    verify(exactly = 1) { assessmentService.findByUuid(assessment.uuid) }
    verify(exactly = 1) { eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { stateService.persist(state) }
    verify(exactly = 1) { eventService.save(any<EventEntity<out Event>>()) }

    assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.uuid)
    assertThat(handledEvent.captured.user).isEqualTo(command.user)
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)

    assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

    assertThat(result).isEqualTo(expectedResult)
  }
}
