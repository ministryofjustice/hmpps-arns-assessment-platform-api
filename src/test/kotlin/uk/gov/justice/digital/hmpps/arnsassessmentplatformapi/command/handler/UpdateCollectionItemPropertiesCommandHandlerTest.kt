package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.every
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.UUID

class UpdateCollectionItemPropertiesCommandHandlerTest : AbstractCommandHandlerTest<UpdateCollectionItemPropertiesCommand>() {
  override val handler = UpdateCollectionItemPropertiesCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<UpdateCollectionItemPropertiesCommand>(
      name = "It handles the command",
    ).apply {
      command = UpdateCollectionItemPropertiesCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid,
        collectionItemUuid = UUID.randomUUID(),
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = listOf("bar"),
        timeline = timeline,
      )
      expectedEvent = CollectionItemPropertiesUpdatedEvent(
        collectionItemUuid = command.collectionItemUuid,
        added = command.added,
        removed = command.removed,
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Throws<UpdateCollectionItemPropertiesCommand, CollectionItemNotFoundException>(
      name = "Throws when unable to find the collection item",
    ).apply {
      setupMocks = { every { assessmentAggregate.getCollectionWithItem(any()) } returns null }
      command = UpdateCollectionItemPropertiesCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid,
        collectionItemUuid = UUID.randomUUID(),
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = listOf("bar"),
      )
      expectedException = CollectionItemNotFoundException::class
    },
  )
}
