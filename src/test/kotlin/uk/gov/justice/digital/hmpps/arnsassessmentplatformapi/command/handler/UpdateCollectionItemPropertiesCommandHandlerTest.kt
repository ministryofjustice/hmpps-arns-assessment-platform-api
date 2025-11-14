package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import java.util.UUID

class UpdateCollectionItemPropertiesCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = UpdateCollectionItemPropertiesCommandHandler::class
  override val command = UpdateCollectionItemPropertiesCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    collectionItemUuid = UUID.randomUUID(),
    added = mapOf("foo" to listOf("foo_value")),
    removed = listOf("bar"),
    timeline = timeline,
  )
  override val expectedEvent = CollectionItemPropertiesUpdatedEvent(
    collectionItemUuid = command.collectionItemUuid,
    added = command.added,
    removed = command.removed,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
