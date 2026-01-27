package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.UUID

class UpdateCollectionItemPropertiesCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = UpdateCollectionItemPropertiesCommandHandler::class
  override val command = UpdateCollectionItemPropertiesCommand(
    user = commandUser,
    assessmentUuid = assessment.uuid,
    collectionItemUuid = UUID.randomUUID(),
    added = mapOf("foo" to SingleValue("foo_value")),
    removed = listOf("bar"),
    timeline = timeline,
  )
  override val expectedEvent = CollectionItemPropertiesUpdatedEvent(
    collectionItemUuid = command.collectionItemUuid,
    added = command.added,
    removed = command.removed,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
