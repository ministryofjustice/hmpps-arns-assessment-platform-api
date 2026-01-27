package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.UUID

class UpdateCollectionItemAnswersCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = UpdateCollectionItemAnswersCommandHandler::class
  override val command = UpdateCollectionItemAnswersCommand(
    user = commandUser,
    assessmentUuid = assessment.uuid,
    collectionItemUuid = UUID.randomUUID(),
    added = mapOf("foo" to SingleValue("foo_value")),
    removed = listOf("bar"),
    timeline = timeline,
  )
  override val expectedEvent = CollectionItemAnswersUpdatedEvent(
    collectionItemUuid = command.collectionItemUuid,
    added = command.added,
    removed = command.removed,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
