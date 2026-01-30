package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue

class UpdateAssessmentAnswersCommandHandlerTest : AbstractCommandHandlerTest() {

  override val handler = UpdateAssessmentAnswersCommandHandler::class
  override val command = UpdateAssessmentAnswersCommand(
    user = commandUser,
    assessmentUuid = assessment.uuid,
    added = mapOf("foo" to SingleValue("foo_value")),
    removed = listOf("bar"),
    timeline = timeline,
  )
  override val expectedEvent = AssessmentAnswersUpdatedEvent(
    added = command.added,
    removed = command.removed,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
