package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

/*
 * This test is intended for manual checking/debugging using a profiler
 */
class BatchInsertsTest : IntegrationTestBase() {

  @Test
  fun `entities are inserted in batches`() {
    val commands = mutableListOf<RequestableCommand>()

    CreateAssessmentCommand(
      user = newUser(),
      assessmentType = "TEST",
      formVersion = "1",
    ).apply(commands::add)

    for (i in 1..100) {
      UpdateAssessmentAnswersCommand(
        user = newUser(),
        assessmentUuid = Reference("@0"),
        added = mapOf("a" to SingleValue("b")),
        removed = emptyList(),
      )

      CreateCollectionCommand(
        user = newUser(),
        assessmentUuid = Reference("@0"),
        name = "A $i",
        parentCollectionItemUuid = null,
      ).apply(commands::add)

      AddCollectionItemCommand(
        collectionUuid = Reference("@${commands.size - 1}"),
        answers = mapOf("a" to SingleValue("b")),
        properties = emptyMap(),
        index = 0,
        user = newUser(),
        assessmentUuid = Reference("@0"),
      ).apply(commands::add)

      val collectionItemRef = Reference("@${commands.size - 1}")

      CreateCollectionCommand(
        user = newUser(),
        assessmentUuid = Reference("@0"),
        name = "Nested Collection $i",
        parentCollectionItemUuid = collectionItemRef,
      ).apply(commands::add)

      AddCollectionItemCommand(
        collectionUuid = Reference("@${commands.size - 1}"),
        answers = mapOf("a" to SingleValue("b")),
        properties = emptyMap(),
        index = 0,
        user = newUser(),
        assessmentUuid = Reference("@0"),
      ).apply(commands::add)

      RemoveCollectionItemCommand(
        collectionItemUuid = collectionItemRef,
        user = newUser(),
        assessmentUuid = Reference("@0"),
      )
    }

    val response = command(*commands.toList().toTypedArray())

    assertIs<CreateAssessmentCommandResult>(
      response.commands.find { it.request is CreateAssessmentCommand }?.result,
    )
  }

  private fun newUser() = UserDetails(
    id = "FOO_USER_${UUID.randomUUID()}",
    name = "Foo User",
    authSource = AuthSource.HMPPS_AUTH,
  )
}
