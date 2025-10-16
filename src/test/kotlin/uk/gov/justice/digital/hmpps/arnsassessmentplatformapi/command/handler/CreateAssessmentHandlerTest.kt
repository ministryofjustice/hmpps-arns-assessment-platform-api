package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.test.assertIs

class CreateAssessmentHandlerTest {
  val assessmentRepository: AssessmentRepository = mockk()
  val eventBus: EventBus = mockk()

  val handler = CreateAssessmentHandler(
    assessmentRepository = assessmentRepository,
    eventBus = eventBus,
  )

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(CreateAssessment::class)
  }

  @Test
  fun `it handles the CreateAssessment command`() {
    val command = CreateAssessment(
      user = User("FOO_USER", "Foo User"),
    )

    val assessment = slot<AssessmentEntity>()
    every { assessmentRepository.save(capture(assessment)) } answers { firstArg() }

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { assessmentRepository.save(any<AssessmentEntity>()) }
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.assessment.uuid).isEqualTo(command.assessmentUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    assertIs<AssessmentCreated>(event.captured.data)
  }
}
