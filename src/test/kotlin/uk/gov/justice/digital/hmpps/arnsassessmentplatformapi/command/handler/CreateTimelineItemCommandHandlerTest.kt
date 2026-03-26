package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateTimelineItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import java.time.LocalDateTime
import java.util.UUID

class CreateTimelineItemCommandHandlerTest {
  val services: CommandHandlerServiceBundle = mockk()
  val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)

  val handler = CreateTimelineItemCommandHandler(services)

  val command = CreateTimelineItemCommand(
    user = commandUser,
    assessmentUuid = UUID.randomUUID().toReference(),
    timestamp = LocalDateTime.now(),
    timeline = Timeline(
      type = "test",
      data = mapOf("bar" to "baz"),
    ),
  )

  val assessment: AssessmentEntity = mockk()

  val expectedResult = CommandSuccessCommandResult()

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
    every { services.assessment.findBy(command.assessmentUuid.value) } answers { assessment }
    every { services.userDetails.findOrCreate(commandUser) } returns user

    val timeline = slot<TimelineEntity>()

    every { services.timeline.save(capture(timeline)) } answers { firstArg() }

    val result = handler.handle(command)

    verify(exactly = 1) { services.assessment.findBy(command.assessmentUuid.value) }
    verify(exactly = 1) { services.userDetails.findOrCreate(commandUser) }

    assertThat(timeline.captured.data).isEmpty()
    assertThat(timeline.captured.eventType).isNull()
    assertThat(timeline.captured.customType).isEqualTo("test")
    assertThat(timeline.captured.customData).containsExactlyEntriesOf(mapOf("bar" to "baz"))
    assertThat(timeline.captured.createdAt).isEqualTo(command.timestamp)
    assertThat(timeline.captured.assessment).isSameAs(assessment)
    assertThat(timeline.captured.user).isSameAs(user)

    assertThat(result).isEqualTo(expectedResult)
  }
}
