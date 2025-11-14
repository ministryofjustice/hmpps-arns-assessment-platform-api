package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent

class UpdateAssessmentAnswersCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = UpdateAssessmentAnswersCommandHandler::class
  override val command = UpdateAssessmentAnswersCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    added = mapOf("foo" to listOf("foo_value")),
    removed = listOf("bar"),
    timeline = timeline,
  )
  override val expectedEvent = AssessmentAnswersUpdatedEvent(
    added = command.added,
    removed = command.removed,
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
