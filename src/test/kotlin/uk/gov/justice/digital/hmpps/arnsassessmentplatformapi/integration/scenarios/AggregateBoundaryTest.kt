package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.scenarios

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class AggregateBoundaryTest(
  @Autowired
  private val eventRepository: EventRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
) : IntegrationTestBase() {
  val collectionUuid: UUID = UUID.randomUUID()
  val collectionItemUuid: UUID = UUID.randomUUID()

  var now: LocalDateTime = LocalDateTime.parse("2025-10-06T10:15:30")
  fun nextDay(): LocalDateTime {
    now = now.plusDays(1); return now
  }

  @Test
  fun `the boundary works`() {
    // initial events 0..3
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

    // remaining events up until the boundary, 4..49
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

    // event 50
    // add a removal for the initial collection item, this should after on the boundary
    val event50 = nextDay()
    backdatedCommand(
      backdateTo = event50,
      RemoveCollectionItemCommand(
        assessmentUuid = Reference(assessmentUuid.toString()),
        collectionItemUuid = Reference(collectionItemUuid.toString()),
        user = testUserDetails,
      ),
    )

    // event 51
    val event51 = nextDay()
    backdatedCommand(
      backdateTo = event51,
      UpdateAssessmentAnswersCommand(
        assessmentUuid = Reference(assessmentUuid.toString()),
        user = testUserDetails,
        added = mapOf("FOO" to SingleValue("event 51")),
        removed = emptyList(),
      ),
    )

    assertIs<AssessmentVersionQueryResult>(
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

    assertIs<AssessmentVersionQueryResult>(
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
  }
}
