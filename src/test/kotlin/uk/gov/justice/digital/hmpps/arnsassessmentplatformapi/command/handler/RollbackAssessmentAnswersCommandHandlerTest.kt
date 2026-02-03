package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent

class RollbackAssessmentAnswersCommandHandlerTest : AbstractCommandHandlerTest<RollbackCommand>() {
  override val handler = RollbackCommandHandler::class

  override val scenarios = listOf(
    Scenario.Executes<RollbackCommand>(
      name = "It handles the command",
    ).apply {
      command = RollbackCommand(
        user = commandUser,
        assessmentUuid = assessment.uuid,
        pointInTime = Clock.now(),
        timeline = timeline,
      )
      expectedEvent = AssessmentRolledBackEvent(
        rolledBackTo = command.pointInTime,
      )
      expectedResult = CommandSuccessCommandResult()
    },
  )
}
