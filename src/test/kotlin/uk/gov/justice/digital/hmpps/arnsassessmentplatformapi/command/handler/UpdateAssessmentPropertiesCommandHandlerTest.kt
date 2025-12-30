package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class UpdateAssessmentPropertiesCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = UpdateAssessmentPropertiesCommandHandler::class
  override val command = UpdateAssessmentPropertiesCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    added = mapOf("foo" to SingleValue("bar")),
    removed = listOf("baz"),
    timeline = timeline,
  )
  override val expectedEvent = AssessmentPropertiesUpdatedEvent(
    added = command.added,
    removed = command.removed,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
