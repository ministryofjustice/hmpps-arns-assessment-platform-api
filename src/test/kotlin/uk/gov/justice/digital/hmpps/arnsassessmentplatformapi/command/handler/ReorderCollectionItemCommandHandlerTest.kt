package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.every
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import java.util.UUID

class ReorderCollectionItemCommandHandlerTest : AbstractCommandHandlerTest<ReorderCollectionItemCommand>() {
  override val handler = ReorderCollectionItemCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<ReorderCollectionItemCommand>(
      name = "It handles the command",
    ).apply {
      command = ReorderCollectionItemCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        collectionItemUuid = UUID.randomUUID().toReference(),
        index = 0,
        timeline = timeline,
      )
      expectedEvent = CollectionItemReorderedEvent(
        collectionItemUuid = command.collectionItemUuid.value,
        index = command.index,
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Throws<ReorderCollectionItemCommand, CollectionItemNotFoundException>(
      name = "Throws when unable to find the collection item",
    ).apply {
      setupMocks = { every { assessmentAggregate.getCollectionWithItem(any()) } returns null }
      command = ReorderCollectionItemCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        collectionItemUuid = UUID.randomUUID().toReference(),
        index = 0,
        timeline = timeline,
      )
      expectedException = CollectionItemNotFoundException::class
    },
  )
}
