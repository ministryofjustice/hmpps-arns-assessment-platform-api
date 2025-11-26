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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService

class CreateAssessmentCommandHandlerTest {
  val assessmentRepository: AssessmentRepository = mockk()
  val eventBus: EventBus = mockk()
  val eventService: EventService = mockk()
  val stateService: StateService = mockk()

  val handler = CreateAssessmentCommandHandler(
    assessmentRepository = assessmentRepository,
    eventBus = eventBus,
    eventService = eventService,
    stateService = stateService,
  )

  val command = CreateAssessmentCommand(
    user = User("FOO_USER", "Foo User"),
    formVersion = "1",
    properties = mapOf("foo" to listOf("bar")),
    timeline = Timeline(
      type = "test",
      data = mapOf("bar" to listOf("baz")),
    ),
  )

  val expectedEvent = AssessmentCreatedEvent(
    formVersion = "1",
    properties = command.properties,
    timeline = command.timeline,
  )

  val expectedResult = CreateAssessmentCommandResult(
    assessmentUuid = command.assessmentUuid,
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
    val assessment = slot<AssessmentEntity>()
    every { assessmentRepository.save(capture(assessment)) } answers { firstArg() }

    val handledEvent = slot<EventEntity<out Event>>()
    val persistedEvent = slot<EventEntity<out Event>>()
    val state: State = mockk()

    every { eventBus.handle(capture(handledEvent)) } returns state
    every { stateService.persist(state) } just Runs
    every { eventService.save(capture(persistedEvent)) } answers { firstArg() }

    val result = handler.handle(command)

    verify(exactly = 1) { assessmentRepository.save(any<AssessmentEntity>()) }
    verify(exactly = 1) { eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { stateService.persist(state) }
    verify(exactly = 1) { eventService.save(any<EventEntity<out Event>>()) }

    assertThat(assessment.captured.uuid).isEqualTo(command.assessmentUuid)
    assertThat(handledEvent.captured.assessment.uuid).isEqualTo(assessment.captured.uuid)
    assertThat(handledEvent.captured.user).isEqualTo(command.user)
    assertThat(handledEvent.captured.data).isEqualTo(expectedEvent)

    assertThat(handledEvent.captured).isEqualTo(persistedEvent.captured)

    assertThat(result).isEqualTo(expectedResult)
  }
}
