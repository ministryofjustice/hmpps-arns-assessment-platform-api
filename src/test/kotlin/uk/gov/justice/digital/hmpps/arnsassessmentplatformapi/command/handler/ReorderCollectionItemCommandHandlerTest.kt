package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ReorderCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import java.util.UUID

class ReorderCollectionItemCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = ReorderCollectionItemCommandHandler::class
  override val command = ReorderCollectionItemCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    collectionItemUuid = UUID.randomUUID(),
    index = 0,
    timeline = timeline,
  )
  override val expectedEvent = CollectionItemReorderedEvent(
    collectionItemUuid = command.collectionItemUuid,
    index = command.index,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
