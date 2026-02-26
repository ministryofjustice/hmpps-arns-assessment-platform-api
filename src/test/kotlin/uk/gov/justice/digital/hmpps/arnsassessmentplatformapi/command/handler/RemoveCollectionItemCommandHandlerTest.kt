package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.every
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import java.util.*

class RemoveCollectionItemCommandHandlerTest : AbstractCommandHandlerTest<RemoveCollectionItemCommand>() {
  override val handler = RemoveCollectionItemCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<RemoveCollectionItemCommand>(
      name = "It handles the command",
    ).apply {
      command = RemoveCollectionItemCommand(
        user = commandUser,
        collectionItemUuid = UUID.randomUUID().toReference(),
        assessmentUuid = assessment.uuid.toReference(),
        timeline = timeline,
      )
      expectedEvent = CollectionItemRemovedEvent(
        collectionItemUuid = command.collectionItemUuid.value,
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Throws<RemoveCollectionItemCommand, CollectionItemNotFoundException>(
      name = "Throws when unable to find the collection item",
    ).apply {
      setupMocks = { every { assessmentAggregate.getCollectionWithItem(any()) } returns null }
      command = RemoveCollectionItemCommand(
        user = commandUser,
        collectionItemUuid = UUID.randomUUID().toReference(),
        assessmentUuid = assessment.uuid.toReference(),
        timeline = timeline,
      )
      expectedException = CollectionItemNotFoundException::class
    },
  )
}
