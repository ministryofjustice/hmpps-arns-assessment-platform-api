package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.every
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.*

class UpdateCollectionItemAnswersCommandHandlerTest : AbstractCommandHandlerTest<UpdateCollectionItemAnswersCommand>() {
  override val handler = UpdateCollectionItemAnswersCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<UpdateCollectionItemAnswersCommand>(
      name = "It handles the command",
    ).apply {
      command = UpdateCollectionItemAnswersCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid,
        collectionItemUuid = UUID.randomUUID(),
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = listOf("bar"),
        timeline = timeline,
      )
      expectedEvent = CollectionItemAnswersUpdatedEvent(
        collectionItemUuid = command.collectionItemUuid,
        added = command.added,
        removed = command.removed,
      )
      expectedResult = CommandSuccessCommandResult()
    },
    Scenario.Throws<UpdateCollectionItemAnswersCommand, CollectionItemNotFoundException>(
      name = "Throws when unable to find the collection item",
    ).apply {
      setupMocks = { every { assessmentAggregate.getCollectionWithItem(any()) } returns null }
      command = UpdateCollectionItemAnswersCommand(
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
