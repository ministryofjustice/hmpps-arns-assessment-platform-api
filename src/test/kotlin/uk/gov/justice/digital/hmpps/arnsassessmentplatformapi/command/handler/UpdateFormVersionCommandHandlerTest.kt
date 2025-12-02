package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateFormVersionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent

class UpdateFormVersionCommandHandlerTest : AbstractCommandHandlerTest() {
  override val handler = UpdateFormVersionCommandHandler::class
  override val command = UpdateFormVersionCommand(
    user = user,
    assessmentUuid = assessment.uuid,
    version = "2",
    timeline = timeline,
  )
  override val expectedEvent = FormVersionUpdatedEvent(
    version = "2",
    timeline = command.timeline,
  )
  override val expectedResult = CommandSuccessCommandResult()
}
