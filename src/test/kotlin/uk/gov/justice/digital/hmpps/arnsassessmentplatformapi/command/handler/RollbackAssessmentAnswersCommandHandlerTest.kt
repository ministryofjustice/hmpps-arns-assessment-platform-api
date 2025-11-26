package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RollBackAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent

class RollbackAssessmentAnswersCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = RollbackAssessmentAnswersCommandHandler::class
  override val command = RollBackAssessmentAnswersCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    pointInTime = Clock.now(),
    timeline = timeline,
  )
  override val expectedEvent = AssessmentAnswersRolledBackEvent(
    rolledBackTo = command.pointInTime,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
