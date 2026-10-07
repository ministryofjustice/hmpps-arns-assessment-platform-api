package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.RemoveCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.AddCollectionItemCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.Reference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class AggregateBoundaryTest : IntegrationTestBase() {
  val collectionUuid: UUID = UUID.randomUUID()
  val collectionItemUuid: UUID = UUID.randomUUID()

  var now: LocalDateTime = LocalDateTime.parse("2025-10-06T10:15:30")
  fun nextDay(): LocalDateTime {
    now = now.plusDays(1)
    return now
  }

  @Test
  fun `we do not mutate the previous state`() {
    // create the initial events 0..3
    val response = backdatedCommand(
      backdateTo = nextDay(),
      CreateAssessmentCommand(
        formVersion = "v1.0",
        assessmentType = "AAP_PLAN",
        user = testUserDetails,
      ),
      CreateCollectionCommand(
        name = "TEST_COLLECTION_NAME",
        parentCollectionItemUuid = null,
        user = testUserDetails,
        assessmentUuid = Reference("@0"),
      ),
      AddCollectionItemCommand(
        collectionUuid = Reference("@1"),
        user = testUserDetails,
        answers = emptyMap(),
        properties = emptyMap(),
        index = 0,
        assessmentUuid = Reference("@0"),
      ),
    )

    val assessmentUuid = assertIs<CreateAssessmentCommandResult>(response.commands[0].result).assessmentUuid
    val collectionItemUuid = assertIs<AddCollectionItemCommandResult>(response.commands[2].result).collectionItemUuid

    // create remaining events up until the boundary, 4..49
    for (i in 4..49) {
      backdatedCommand(
        backdateTo = nextDay(),
        UpdateAssessmentAnswersCommand(
          assessmentUuid = Reference(assessmentUuid.toString()),
          user = testUserDetails,
          added = mapOf("FOO" to SingleValue("event $i")),
          removed = emptyList(),
        ),
      )
    }

    val event49 = LocalDateTime.from(now)

    // create event 50
    // add a removal for the initial collection item, this should be after the boundary
    val event50 = nextDay()
    backdatedCommand(
      backdateTo = event50,
      RemoveCollectionItemCommand(
        assessmentUuid = Reference(assessmentUuid.toString()),
        collectionItemUuid = Reference(collectionItemUuid.toString()),
        user = testUserDetails,
      ),
    )

    // create event 51
    val event51 = nextDay()
    backdatedCommand(
      backdateTo = event51,
      UpdateAssessmentAnswersCommand(
        assessmentUuid = Reference(assessmentUuid.toString()),
        user = testUserDetails,
        added = mapOf("FOO" to SingleValue("event 51")),
        removed = emptyList(),
      ),
      UpdateAssessmentAnswersCommand(
        assessmentUuid = Reference(assessmentUuid.toString()),
        user = testUserDetails,
        added = mapOf("FOO" to SingleValue("latest")),
        removed = emptyList(),
      ),
    )

    // query for the version following event 49, just before the boundary
    val versionBeforeTheBoundary = assertIs<AssessmentVersionQueryResult>(
      query(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(
            uuid = assessmentUuid,
          ),
          timestamp = event49.plusSeconds(1),
        ),
      )
        .expectStatus().isOk
        .expectBody<QueriesResponse>()
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    versionBeforeTheBoundary.collections.first { it.name == "TEST_COLLECTION_NAME" }.items.let {
      assert(it.size == 1) { "We should not have mutated a collection in the state before the boundary" }
    }

    assertIs<SingleValue>(versionBeforeTheBoundary.answers["FOO"]).value.let {
      assert(it == "event 49") { "We should not have mutated answers in the state before the boundary" }
    }

    // query for the frontier version
    val latestVersion = assertIs<AssessmentVersionQueryResult>(
      query(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(
            uuid = assessmentUuid,
          ),
        ),
      )
        .expectStatus().isOk
        .expectBody<QueriesResponse>()
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    latestVersion.collections.first { it.name == "TEST_COLLECTION_NAME" }.items.let {
      assert(it.isEmpty()) { "The collection should be empty in the latest version" }
    }

    assertIs<SingleValue>(latestVersion.answers["FOO"]).value.let {
      assert(it == "latest") { "The answer should have been updated in the latest version" }
    }

    // query for a version immediately after the boundary, this should force the creation of a new aggregate based
    // on a previously created one.
    val versionJustAfterBoundary = assertIs<AssessmentVersionQueryResult>(
      query(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(
            uuid = assessmentUuid,
          ),
          timestamp = event50.plusSeconds(1),
        ),
      )
        .expectStatus().isOk
        .expectBody<QueriesResponse>()
        .returnResult()
        .responseBody!!
        .queries.first().result,
    )

    versionJustAfterBoundary.collections.first { it.name == "TEST_COLLECTION_NAME" }.items.let {
      assert(it.isEmpty()) { "The collection should be empty immediately after event 50 is applied" }
    }
  }
}
