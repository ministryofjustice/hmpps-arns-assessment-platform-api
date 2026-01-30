package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateCollectionCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import java.util.UUID

class CreateCollectionCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = CreateCollectionCommandHandler::class
  override val command = CreateCollectionCommand(
    user = commandUser,
    name = "TEST_COLLECTION",
    parentCollectionItemUuid = UUID.randomUUID(),
    assessmentUuid = assessment.uuid,
    timeline = timeline,
  )
  override val expectedEvent = CollectionCreatedEvent(
    collectionUuid = command.collectionUuid,
    name = command.name,
    parentCollectionItemUuid = command.parentCollectionItemUuid,
    timeline = command.timeline,
  )
  override val expectedResult = CreateCollectionCommandResult(
    collectionUuid = command.collectionUuid,
  )
}
