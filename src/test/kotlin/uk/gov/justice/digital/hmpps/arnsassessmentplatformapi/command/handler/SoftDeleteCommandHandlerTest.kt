package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.State
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler.common.CommandHandlerServiceBundle
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.PersistenceContext
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.EventService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.StateService
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.TimelineService
import java.time.LocalDateTime
import java.util.UUID

class SoftDeleteCommandHandlerTest {
  val services: CommandHandlerServiceBundle = mockk()
  val persistenceContext: PersistenceContext = mockk()
  val eventService: EventService = mockk()
  val timelineService: TimelineService = mockk()
  val stateService: StateService = mockk()

  val now: LocalDateTime = LocalDateTime.now()
  val pointInTime: LocalDateTime = now.minusHours(1)
  val commandUser = UserDetails("FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val user = UserDetailsEntity(1, UUID.randomUUID(), "FOO_USER", "Foo User", AuthSource.NOT_SPECIFIED)
  val assessment = AssessmentEntity(type = "TEST", createdAt = now.minusDays(1))

  val rebuiltState: State = mockk()

  val handler = SoftDeleteCommandHandler(services)

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { services.persistenceContext } returns persistenceContext
    every { services.clock.requestDateTime() } returns now
    every { persistenceContext.findAssessment(assessment.uuid) } returns assessment
    every { persistenceContext.eventService } returns eventService
    every { persistenceContext.timelineService } returns timelineService
    every { persistenceContext.stateService } returns stateService
    every { eventService.softDelete(any(), any()) } just Runs
    every { timelineService.softDelete(any(), any()) } just Runs
    every { stateService.delete(any()) } just Runs
  }

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(SoftDeleteCommand::class)
  }

  @Nested
  inner class Handle {
    @Test
    fun `should soft delete events and timeline entries from the point in time`() {
      val command = SoftDeleteCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        pointInTime = pointInTime,
      )

      every { stateService.rebuildFromEvents(assessment, null) } returns rebuiltState
      every { stateService.persist(any()) } just Runs

      handler.handle(command)

      verify(exactly = 1) { eventService.softDelete(assessment.uuid, pointInTime) }
      verify(exactly = 1) { timelineService.softDelete(assessment.uuid, pointInTime) }
    }

    @Test
    fun `should delete aggregates and rebuild state from surviving events`() {
      val command = SoftDeleteCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        pointInTime = pointInTime,
      )

      every { stateService.rebuildFromEvents(assessment, null) } returns rebuiltState
      every { stateService.persist(any()) } just Runs

      handler.handle(command)

      verify(exactly = 1) { stateService.delete(assessment.uuid) }
      verify(exactly = 1) { stateService.rebuildFromEvents(assessment, null) }
      verify(exactly = 1) { stateService.persist(mutableMapOf(assessment.uuid to rebuiltState)) }
    }

    @Test
    fun `should add a timeline entry when timeline is provided`() {
      val timeline = Timeline(type = "SOFT_DELETE", data = mapOf("reason" to "test"))
      val command = SoftDeleteCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        pointInTime = pointInTime,
        timeline = timeline,
      )

      val timelineList = mutableListOf<TimelineEntity>()

      every { stateService.rebuildFromEvents(assessment, null) } returns rebuiltState
      every { stateService.persist(any()) } just Runs
      every { persistenceContext.findUserDetails(commandUser) } returns user
      every { persistenceContext.timeline } returns timelineList

      handler.handle(command)

      assertThat(timelineList).hasSize(1)
      val entry = timelineList.first()
      assertThat(entry.createdAt).isEqualTo(now)
      assertThat(entry.user).isEqualTo(user)
      assertThat(entry.assessment).isEqualTo(assessment)
      assertThat(entry.customType).isEqualTo("SOFT_DELETE")
      assertThat(entry.customData).isEqualTo(mapOf("reason" to "test"))
    }

    @Test
    fun `should not add a timeline entry when timeline is not provided`() {
      val command = SoftDeleteCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        pointInTime = pointInTime,
      )

      every { stateService.rebuildFromEvents(assessment, null) } returns rebuiltState
      every { stateService.persist(any()) } just Runs

      handler.handle(command)

      verify(exactly = 0) { persistenceContext.timeline }
    }

    @Test
    fun `should return CommandSuccessCommandResult`() {
      val command = SoftDeleteCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        pointInTime = pointInTime,
      )

      every { stateService.rebuildFromEvents(assessment, null) } returns rebuiltState
      every { stateService.persist(any()) } just Runs

      val result = handler.handle(command)

      assertThat(result).isEqualTo(CommandSuccessCommandResult())
    }
  }
}
