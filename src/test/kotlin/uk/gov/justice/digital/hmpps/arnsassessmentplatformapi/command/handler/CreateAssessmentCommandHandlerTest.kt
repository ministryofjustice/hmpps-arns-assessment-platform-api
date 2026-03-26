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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.clock.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.TimelinesResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import java.time.LocalDateTime
import java.util.UUID

class CreateAssessmentCommandHandlerTest {
  val eventBus: EventBus = mockk()
  val clock: Clock = mockk()

  val services = CommandHandlerServiceBundle(
    eventBus = eventBus,
    clock = clock,
  )

  val now: LocalDateTime = LocalDateTime.now()
  val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)

  val handler = CreateAssessmentCommandHandler(services)

  val command = CreateAssessmentCommand(
    user = commandUser,
    assessmentType = "TEST",
    formVersion = "1",
    properties = mapOf("foo" to SingleValue("bar")),
    flags = listOf("SAN_BETA"),
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
      flags = listOf("SAN_BETA"),
    ),
    AssignedToUserEvent(
      userUuid = user.uuid,
    ),
  )

  val expectedResult = CreateAssessmentCommandResult(
    assessmentUuid = command.assessmentUuid.value,
  )

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { clock.now() } returns now
    every { clock.requestDateTime() } returns now
  }

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(command::class)
  }

  @Test
  fun `it handles the command`() {
    val assessment = slot<AssessmentEntity>()
    every { eventBus.persistenceContext.assessments.add(capture(assessment)) } answers { firstArg() }

    val handledEvents = mutableListOf<EventEntity<out Event>>()

    val assessmentCreatedTimelinesResolver: TimelinesResolver = mockk()
    val assignedToUserTimelinesResolver: TimelinesResolver = mockk()

    every { assessmentCreatedTimelinesResolver.createTimeline(command.timeline) } just Runs
    every { assignedToUserTimelinesResolver.createTimeline(command.timeline) } just Runs

    every { eventBus.handle(capture(handledEvents)) } returns assessmentCreatedTimelinesResolver andThen assignedToUserTimelinesResolver

    every { eventBus.persistenceContext.findUserDetails(commandUser) } returns user

    val result = handler.handle(command)

    val expectedIdentifier = ExternalIdentifier("CRN123", IdentifierType.CRN, "TEST")

    verify(exactly = 1) { eventBus.persistenceContext.assessments.add(any<AssessmentEntity>()) }
    verify(exactly = 1) { eventBus.persistenceContext.findUserDetails(commandUser) }
    verify(exactly = 2) { eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { assessmentCreatedTimelinesResolver.createTimeline(command.timeline) }
    verify(exactly = 1) { assignedToUserTimelinesResolver.createTimeline(command.timeline) }

    assertThat(assessment.captured.uuid).isEqualTo(command.assessmentUuid.value)
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
      assertThat(handledEvent.createdAt).isEqualTo(assessment.captured.createdAt)
    }

    assertThat(result).isEqualTo(expectedResult)
  }
}
