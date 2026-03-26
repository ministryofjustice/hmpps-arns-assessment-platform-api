package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import kotlin.test.Test
import kotlin.test.assertIs

class BatchInsertsTest : IntegrationTestBase() {

  @Test
  fun `entities are inserted in batches`() {
    val commands = mutableListOf<RequestableCommand>()

    val getReferenceFrom = { command: RequestableCommand ->
      Reference("@${commands.indexOf(command)}")
    }

    val assessmentCommand = CreateAssessmentCommand(
      user = testUserDetails,
      assessmentType = "TEST",
      formVersion = "1",
    ).apply(commands::add)

    for (i in 1..500) {
      CreateCollectionCommand(
        user = testUserDetails,
        assessmentUuid = getReferenceFrom(assessmentCommand),
        name = "A $i",
        parentCollectionItemUuid = null,
      ).apply(commands::add)
    }

    val response = command(*commands.toList().toTypedArray())

    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(
      response.commands.find { it.request is CreateAssessmentCommand }?.result,
    ).assessmentUuid
  }
}
