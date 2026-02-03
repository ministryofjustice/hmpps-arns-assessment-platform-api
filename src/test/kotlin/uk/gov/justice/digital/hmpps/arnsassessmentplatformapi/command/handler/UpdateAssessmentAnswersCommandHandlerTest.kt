package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class UpdateAssessmentAnswersCommandHandlerTest : AbstractCommandHandlerTest<UpdateAssessmentAnswersCommand>() {
  override val handler = UpdateAssessmentAnswersCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<UpdateAssessmentAnswersCommand>(
      name = "It handles the command",
    ).apply {
      command = UpdateAssessmentAnswersCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid,
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = listOf("bar"),
        timeline = timeline,
      )
      expectedEvent = AssessmentAnswersUpdatedEvent(
        added = command.added,
        removed = command.removed,
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
