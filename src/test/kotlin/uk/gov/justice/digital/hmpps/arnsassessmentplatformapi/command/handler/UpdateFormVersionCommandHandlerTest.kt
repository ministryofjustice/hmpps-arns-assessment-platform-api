package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CollectionService
import java.util.UUID
import kotlin.test.assertIs

class UpdateFormVersionCommandHandlerTest {
  val collectionService: CollectionService = mockk()
  val eventBus: EventBus = mockk()

  val handler = UpdateFormVersionCommandHandler(
    collectionService = collectionService,
    eventBus = eventBus,
  )

  @Test
  fun `it stores the type of the command it is built to handle`() {
    assertThat(handler.type).isEqualTo(UpdateFormVersionCommand::class)
  }

  @Test
  fun `it handles the UpdateFormVersion command`() {
    val command = UpdateFormVersionCommand(
      user = User("FOO_USER", "Foo User"),
      collectionUuid = UUID.randomUUID(),
      version = "1.2",
    )

    val assessment = CollectionEntity(uuid = command.collectionUuid)
    every { collectionService.findByUuid(command.collectionUuid) } returns assessment

    val event = slot<EventEntity>()
    every { eventBus.add(capture(event)) } just Runs

    handler.execute(command)
    verify(exactly = 1) { eventBus.add(any<EventEntity>()) }

    assertThat(event.captured.collection.uuid).isEqualTo(command.collectionUuid)
    assertThat(event.captured.user).isEqualTo(command.user)
    val eventData = assertIs<FormVersionUpdatedEvent>(event.captured.data)
    assertThat(eventData.version).isEqualTo(command.version)
  }
}
