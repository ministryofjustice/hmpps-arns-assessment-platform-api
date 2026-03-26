package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import java.util.UUID
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

    for (i in 1..100) {
      CreateCollectionCommand(
        user = testUserDetails,
        assessmentUuid = getReferenceFrom(assessmentCommand),
        name = "A $i",
        parentCollectionItemUuid = null,
      ).apply(commands::add)

      if (i == 20) {
        AddCollectionItemCommand(
          collectionUuid = Reference("@2"),
          answers = mapOf("a" to SingleValue("b")),
          properties = emptyMap(),
          index = 0,
          user = UserDetails(id = "FOO_USER_${UUID.randomUUID()}", name = "Foo User", authSource = AuthSource.HMPPS_AUTH),
          assessmentUuid = Reference("@0")
        ).apply(commands::add)

        CreateAssessmentCommand(
          user = UserDetails(id = "FOO_USER_${UUID.randomUUID()}", name = "Foo User", authSource = AuthSource.HMPPS_AUTH),
          assessmentType = "TEST",
          formVersion = "1",
        ).apply(commands::add)
      }
    }

    val response = command(*commands.toList().toTypedArray())

    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(
      response.commands.find { it.request is CreateAssessmentCommand }?.result,
    ).assessmentUuid
  }
}
