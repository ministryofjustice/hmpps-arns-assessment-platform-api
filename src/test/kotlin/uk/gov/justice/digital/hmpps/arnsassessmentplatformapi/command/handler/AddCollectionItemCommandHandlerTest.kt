package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.UUID

class AddCollectionItemCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = AddCollectionItemCommandHandler::class
  override val command = AddCollectionItemCommand(
    collectionUuid = UUID.randomUUID(),
    answers = mapOf("foo" to SingleValue("bar")),
    properties = mapOf("bar" to SingleValue("baz")),
    index = 2,
    user = commandUser,
    assessmentUuid = assessment.uuid,
    timeline = timeline,
  )
  override val expectedEvent = CollectionItemAddedEvent(
    collectionItemUuid = command.collectionItemUuid,
    collectionUuid = command.collectionUuid,
    answers = command.answers,
    properties = command.properties,
    index = command.index,
    timeline = command.timeline,
  )
  override val expectedResult = AddCollectionItemCommandResult(
    collectionItemUuid = command.collectionItemUuid,
  )
}
