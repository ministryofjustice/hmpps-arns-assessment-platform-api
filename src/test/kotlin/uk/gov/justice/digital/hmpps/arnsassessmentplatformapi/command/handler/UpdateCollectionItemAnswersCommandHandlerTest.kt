package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
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
        assessmentUuid = assessment.uuid.toReference(),
        collectionItemUuid = UUID.randomUUID().toReference(),
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = listOf("bar"),
        timeline = timeline,
      )
      expectedEvent = CollectionItemAnswersUpdatedEvent(
        collectionItemUuid = command.collectionItemUuid.value,
        added = command.added,
        removed = command.removed,
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
