package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import io.mockk.every
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.CollectionNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.UUID

class AddCollectionItemCommandHandlerTest : AbstractCommandHandlerTest<AddCollectionItemCommand>() {
  val collectionUuid: UUID = UUID.randomUUID()
  val collectionItemUuid: UUID = UUID.randomUUID()

  override val handler = AddCollectionItemCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<AddCollectionItemCommand>(
      name = "It handles the command",
    ).apply {
      command = AddCollectionItemCommand(
        collectionUuid = collectionUuid,
        answers = mapOf("foo" to SingleValue("bar")),
        properties = mapOf("bar" to SingleValue("baz")),
        index = 2,
        user = commandUser,
        assessmentUuid = assessment.uuid,
        timeline = timeline,
      )

      expectedEvent = CollectionItemAddedEvent(
        collectionItemUuid = command.collectionItemUuid,
        collectionUuid = command.collectionUuid,
        answers = command.answers,
        properties = command.properties,
        index = command.index,
      )

      expectedResult = AddCollectionItemCommandResult(
        collectionItemUuid = command.collectionItemUuid,
      )
    },
    Scenario.Throws<AddCollectionItemCommand, CollectionNotFoundException>(
      name = "Throws when unable to find the collection",
    ).apply {
      setupMocks = { every { assessmentAggregate.getCollection(any()) } returns null }
      command = AddCollectionItemCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid,
        answers = mapOf("foo" to SingleValue("bar")),
        properties = mapOf("bar" to SingleValue("baz")),
        timeline = timeline,
        collectionUuid = collectionUuid,
        index = 0,
      )
      expectedException = CollectionNotFoundException::class
    },
  )
}
