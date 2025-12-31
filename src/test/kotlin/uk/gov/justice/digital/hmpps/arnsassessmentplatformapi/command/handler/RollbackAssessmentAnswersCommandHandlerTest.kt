package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollbackCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentRolledBackEvent

class RollbackAssessmentAnswersCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = RollbackCommandHandler::class
  override val command = RollbackCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    pointInTime = Clock.now(),
    timeline = timeline,
  )
  override val expectedEvent = AssessmentRolledBackEvent(
    rolledBackTo = command.pointInTime,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
