package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import java.util.UUID

class CreateCollectionCommandHandlerTest : AbstractCommandHandlerTest<CreateCollectionCommand>() {
  override val handler = CreateCollectionCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<CreateCollectionCommand>(
      name = "It handles the command",
    ).apply {
      command = CreateCollectionCommand(
        user = commandUser,
        name = "TEST_COLLECTION",
        parentCollectionItemUuid = UUID.randomUUID().toReference(),
        assessmentUuid = assessment.uuid.toReference(),
        timeline = timeline,
      )
      expectedEvent = CollectionCreatedEvent(
        collectionUuid = command.collectionUuid,
        name = command.name,
        parentCollectionItemUuid = command.parentCollectionItemUuid?.value,
      )
      expectedResult = CreateCollectionCommandResult(
        collectionUuid = command.collectionUuid,
      )
    },
  )
}
