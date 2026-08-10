package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent

class UpdateFlagsCommandHandlerTest : AbstractCommandHandlerTest<UpdateFlagsCommand>() {
  override val handler = UpdateFlagsCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<UpdateFlagsCommand>(
      name = "It handles the command",
    ).apply {
      command = UpdateFlagsCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid.toReference(),
        flags = listOf("SAN_BETA"),
        timeline = timeline,
      )
      expectedEvent = AssessmentFlagsUpdatedEvent(
        flags = command.flags,
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
