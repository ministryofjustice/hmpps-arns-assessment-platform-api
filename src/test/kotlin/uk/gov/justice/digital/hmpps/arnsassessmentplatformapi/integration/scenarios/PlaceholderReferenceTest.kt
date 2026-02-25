package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.Answers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RequestableCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaceholderReferenceTest : IntegrationTestBase() {

  @Test
  fun `created UUIDs can be referenced in subsequent commands within the same request`() {
    val commands = mutableListOf<RequestableCommand>()

    val getReferenceFrom = { command: RequestableCommand ->
      Reference("@${commands.indexOf(command)}")
    }

    val assessmentCommand = CreateAssessmentCommand(
      user = testUserDetails,
      assessmentType = "TEST",
      formVersion = "1",
    ).apply(commands::add)

    val collectionACommand = CreateCollectionCommand(
      user = testUserDetails,
      assessmentUuid = getReferenceFrom(assessmentCommand),
      name = "A",
      parentCollectionItemUuid = null,
    ).apply(commands::add)

    val collectionAItem1Command = AddCollectionItemCommand(
      user = testUserDetails,
      assessmentUuid = getReferenceFrom(assessmentCommand),
      collectionUuid = getReferenceFrom(collectionACommand),
      answers = mapOf("q1" to SingleValue("a1")),
      properties = emptyMap(),
      index = null,
    ).apply(commands::add)

    AddCollectionItemCommand(
      user = testUserDetails,
      assessmentUuid = getReferenceFrom(assessmentCommand),
      collectionUuid = getReferenceFrom(collectionACommand),
      answers = mapOf("q2" to SingleValue("a2")),
      properties = emptyMap(),
      index = null,
    ).apply(commands::add)

    val collectionBCommand = CreateCollectionCommand(
      user = testUserDetails,
      assessmentUuid = getReferenceFrom(assessmentCommand),
      name = "B",
      parentCollectionItemUuid = getReferenceFrom(collectionAItem1Command),
    ).apply(commands::add)

    AddCollectionItemCommand(
      user = testUserDetails,
      assessmentUuid = getReferenceFrom(assessmentCommand),
      collectionUuid = getReferenceFrom(collectionBCommand),
      answers = mapOf("q3" to SingleValue("a3")),
      properties = emptyMap(),
      index = null,
    ).apply(commands::add)

    val response = command(*commands.toList().toTypedArray())

    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(
      response.commands.find { it.request is CreateAssessmentCommand }?.result,
    ).assessmentUuid

    val result = assertIs<AssessmentVersionQueryResult>(
      query(AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    assertEquals(1, result.collections.size)

    val collectionA = result.collections.first()
    assertEquals("A", collectionA.name)
    assertEquals(2, collectionA.items.size)

    val collectionAItem1 = collectionA.items.first()
    val collectionAItem2 = collectionA.items.last()
    assertEquals<Answers>(mutableMapOf("q1" to SingleValue("a1")), collectionAItem1.answers)
    assertEquals<Answers>(mutableMapOf("q2" to SingleValue("a2")), collectionAItem2.answers)

    assertEquals(1, collectionAItem1.collections.size)

    val collectionB = collectionAItem1.collections.first()
    assertEquals("B", collectionB.name)
    assertEquals(1, collectionB.items.size)
    assertEquals<Answers>(mutableMapOf("q3" to SingleValue("a3")), collectionB.items.first().answers)
  }
}
