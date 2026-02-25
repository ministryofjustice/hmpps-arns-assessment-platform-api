package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class UpdateAssessmentPropertiesCommandHandlerTest : AbstractCommandHandlerTest<UpdateAssessmentPropertiesCommand>() {
  override val handler = UpdateAssessmentPropertiesCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<UpdateAssessmentPropertiesCommand>(
      name = "It handles the command",
    ).apply {
      command = UpdateAssessmentPropertiesCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        added = mapOf("foo" to SingleValue("bar")),
        removed = listOf("baz"),
        timeline = timeline,
      )
      expectedEvent = AssessmentPropertiesUpdatedEvent(
        added = command.added,
        removed = command.removed,
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
