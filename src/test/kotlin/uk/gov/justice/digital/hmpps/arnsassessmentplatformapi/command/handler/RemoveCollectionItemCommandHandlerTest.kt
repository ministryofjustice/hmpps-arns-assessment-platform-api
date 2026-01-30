package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemRemovedEvent
import java.util.UUID

class RemoveCollectionItemCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = RemoveCollectionItemCommandHandler::class
  override val command = RemoveCollectionItemCommand(
    user = commandUser,
    collectionItemUuid = UUID.randomUUID(),
    assessmentUuid = assessment.uuid,
    timeline = timeline,
  )
  override val expectedEvent = CollectionItemRemovedEvent(
    collectionItemUuid = command.collectionItemUuid,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
