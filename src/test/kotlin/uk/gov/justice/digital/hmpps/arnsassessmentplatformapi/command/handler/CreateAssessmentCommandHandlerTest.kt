package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssignedToUserEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.Event
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.TimelinesResolver
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import java.time.LocalDateTime
import java.util.UUID

class CreateAssessmentCommandHandlerTest {
  val services: CommandHandlerServiceBundle = mockk()
  val persistenceContext: PersistenceContext = mockk()

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
    every { services.clock.now() } returns now
    every { services.clock.requestDateTime() } returns now
    every { services.persistenceContext } returns persistenceContext
  }

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(command::class)
  }

  @Test
  fun `it handles the command`() {
    val assessments = mutableListOf<AssessmentEntity>()
    every { persistenceContext.assessments } returns assessments

    val handledEvents = mutableListOf<EventEntity<out Event>>()

    val assessmentCreatedTimelinesResolver: TimelinesResolver = mockk()
    val assignedToUserTimelinesResolver: TimelinesResolver = mockk()

    every { assessmentCreatedTimelinesResolver.createTimeline(command.timeline) } just Runs
    every { assignedToUserTimelinesResolver.createTimeline(command.timeline) } just Runs

    every { services.eventBus.handle(capture(handledEvents)) } returns assessmentCreatedTimelinesResolver andThen assignedToUserTimelinesResolver

    every { persistenceContext.findUserDetails(commandUser) } returns user

    val result = handler.handle(command)

    val expectedIdentifier = ExternalIdentifier("CRN123", IdentifierType.CRN, "TEST")

    verify(exactly = 1) { persistenceContext.findUserDetails(commandUser) }
    verify(exactly = 2) { services.eventBus.handle(any<EventEntity<out Event>>()) }
    verify(exactly = 1) { assessmentCreatedTimelinesResolver.createTimeline(command.timeline) }
    verify(exactly = 1) { assignedToUserTimelinesResolver.createTimeline(command.timeline) }

    assertThat(assessments).hasSize(1)
    val assessment = assessments.first()
    assertThat(assessment.uuid).isEqualTo(command.assessmentUuid.value)
    assertThat(assessment.type).isEqualTo(command.assessmentType)
    assertThat(assessment.identifiers).hasSize(1)
    assessment.identifiers.forEach {
      assertThat(it.toIdentifier()).isEqualTo(expectedIdentifier)
    }

    listOf(
      handledEvents.single { it.data is AssessmentCreatedEvent },
      handledEvents.single { it.data is AssignedToUserEvent },
    ).forEachIndexed { index, handledEvent: EventEntity<out Event> ->
      assertThat(handledEvent.assessment.uuid).isEqualTo(assessment.uuid)
      assertThat(handledEvent.user.userId).isEqualTo(command.user.id)
      assertThat(handledEvent.user.displayName).isEqualTo(command.user.name)
      assertThat(handledEvent.user.authSource).isEqualTo(command.user.authSource)
      assertThat(handledEvent.data).isEqualTo(expectedEvents[index])
      assertThat(handledEvent.createdAt).isEqualTo(assessment.createdAt)
    }

    assertThat(result).isEqualTo(expectedResult)
  }
}
