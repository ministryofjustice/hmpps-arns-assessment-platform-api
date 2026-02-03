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
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.exception.DuplicateExternalIdentifierException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.UserDetailsService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.AssessmentNotFoundException
import java.util.UUID

class CreateAssessmentCommandHandlerTest {
  val assessmentService: AssessmentService = mockk()
  val eventService: EventService = mockk()
  val stateService: StateService = mockk()
  val userDetailsService: UserDetailsService = mockk()
  val timelineService: TimelineService = mockk()
  val eventBus: EventBus = mockk()
  val commandBus: CommandBus = mockk()
  val assessmentAggregate: AssessmentAggregate = mockk()

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

  val assessmentState: AssessmentState = AssessmentState(
    AggregateEntity(
      assessment = AssessmentEntity(type = "TEST"),
      data = assessmentAggregate,
    ),
  )

  val handler = CreateAssessmentCommandHandler(services)

  val command = CreateAssessmentCommand(
    user = commandUser,
    assessmentType = "TEST",
    formVersion = "1",
    properties = mapOf("foo" to SingleValue("bar")),
    identifiers = mapOf(
      IdentifierType.CRN to "CRN123",
    ),
    timeline = Timeline(
      type = "test",
      data = mapOf("bar" to listOf("baz")),
    ),
  )

  val expectedEvents = listOf(
    AssessmentCreatedEvent(
      formVersion = "1",
      properties = command.properties!!,
    ),
    AssignedToUserEvent(
      userUuid = user.uuid,
    ),
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
    every { assessmentService.findBy(any<ExternalIdentifier>()) } answers {
      throw AssessmentNotFoundException(firstArg())
    }
    every { assessmentService.save(capture(assessment)) } answers { firstArg() }

    val persistedEvent = slot<List<EventEntity<out Event>>>()

    val handledEvents = mutableListOf<EventEntity<out Event>>()
    val state: State = mockk()

    every { eventBus.handle(capture(handledEvents)) } returns state

    every { stateService.persist(state) } just Runs
    every { eventService.saveAll(capture(persistedEvent)) } answers { firstArg() }
    every { userDetailsService.findOrCreate(commandUser) } returns user
    every { state[AssessmentAggregate::class] } returns assessmentState
    every { timelineService.saveAll(any()) } answers { firstArg() }

    val result = handler.handle(command)

    val expectedIdentifier = ExternalIdentifier("CRN123", IdentifierType.CRN, "TEST")

    verify(exactly = 1) { assessmentService.findBy(expectedIdentifier) }
    verify(exactly = 1) { assessmentService.save(any<AssessmentEntity>()) }
    verify(exactly = 1) { userDetailsService.findOrCreate(commandUser) }
    verify(exactly = 2) { eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 2) { stateService.persist(state) }
    verify(exactly = 1) { eventService.saveAll(any<List<EventEntity<out Event>>>()) }

    assertThat(assessment.captured.uuid).isEqualTo(command.assessmentUuid)
    assertThat(assessment.captured.type).isEqualTo(command.assessmentType)
    assertThat(assessment.captured.identifiers).hasSize(1)
    assessment.captured.identifiers.forEach {
      assertThat(it.toIdentifier()).isEqualTo(expectedIdentifier)
    }

    listOf(
      handledEvents.single { it.data is AssessmentCreatedEvent },
      handledEvents.single { it.data is AssignedToUserEvent },
    ).forEachIndexed { index, handledEvent: EventEntity<out Event> ->
      assertThat(handledEvent.assessment.uuid).isEqualTo(assessment.captured.uuid)
      assertThat(handledEvent.user.userId).isEqualTo(command.user.id)
      assertThat(handledEvent.user.displayName).isEqualTo(command.user.name)
      assertThat(handledEvent.user.authSource).isEqualTo(command.user.authSource)
      assertThat(handledEvent.data).isEqualTo(expectedEvents[index])

      assertThat(handledEvent).isEqualTo(persistedEvent.captured[index])
      assertThat(handledEvent.createdAt).isEqualTo(assessment.captured.createdAt)
    }

    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `it throws when the provided identifier already exists`() {
    val assessment = slot<AssessmentEntity>()
    every { assessmentService.findBy(any<ExternalIdentifier>()) } answers { AssessmentEntity(type = "TEST") }
    every { assessmentService.save(capture(assessment)) } answers { firstArg() }

    every { eventBus.handle(any<EventEntity<out Event>>()) } returns mockk()
    every { stateService.persist(any()) } just Runs
    every { eventService.save(any<EventEntity<out Event>>()) } answers { firstArg() }

    every { userDetailsService.findOrCreate(commandUser) } returns user

    val exception = assertThrows<DuplicateExternalIdentifierException> { handler.handle(command) }

    val expectedIdentifier = ExternalIdentifier("CRN123", IdentifierType.CRN, "TEST")

    assertThat(exception.developerMessage).isEqualTo("Duplicate identifier: $expectedIdentifier")

    verify(exactly = 1) { assessmentService.findBy(expectedIdentifier) }
    verify(exactly = 0) { assessmentService.save(any<AssessmentEntity>()) }
    verify(exactly = 0) { eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 0) { stateService.persist(any()) }
    verify(exactly = 0) { eventService.save(any<EventEntity<out Event>>()) }
  }
}
