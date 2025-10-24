package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.CollectionRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import kotlin.test.assertIs

class CreateAssessmentCommandHandlerTest {
  val collectionRepository: CollectionRepository = mockk()
  val eventBus: EventBus = mockk()

  val handler = CreateAssessmentCommandHandler(
    collectionRepository = collectionRepository,
    eventBus = eventBus,
  )

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(CreateAssessmentCommand::class)
  }

  @Test
  fun `it handles the CreateAssessment command`() {
    val command = CreateAssessmentCommand(
      user = User("FOO_USER", "Foo User"),
    )

    val assessment = slot<CollectionEntity>()
    every { collectionRepository.save(capture(assessment)) } answers { firstArg() }

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { collectionRepository.save(any<CollectionEntity>()) }
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.collection.uuid).isEqualTo(command.collectionUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    assertIs<AssessmentCreatedEvent>(event.captured.data)
  }
}
