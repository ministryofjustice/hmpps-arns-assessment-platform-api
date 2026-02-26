package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent

class UpdateFormVersionCommandHandlerTest : AbstractCommandHandlerTest<UpdateFormVersionCommand>() {
  override val handler = UpdateFormVersionCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<UpdateFormVersionCommand>(
      name = "It handles the command",
    ).apply {
      command = UpdateFormVersionCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        version = "2",
        timeline = timeline,
      )
      expectedEvent = FormVersionUpdatedEvent(
        version = "2",
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
