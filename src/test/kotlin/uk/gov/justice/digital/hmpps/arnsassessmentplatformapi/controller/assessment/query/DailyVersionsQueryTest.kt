package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.DailyVersionsQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.DailyVersionsQueryResult
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DailyVersionsQueryTest(
  @Autowired
  val entityManager: EntityManager,
  @Autowired
  val timelineRepository: TimelineRepository,
) : IntegrationTestBase() {
  // TODO: If we validate CRN then we may be required to improve this test data
  private val testCrn = UUID.randomUUID().toString()

  private lateinit var assessmentUuid: UUID

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @BeforeAll
  fun setUp() {
    assessmentUuid = assertIs<CreateAssessmentCommandResult>(
      command(
        CreateAssessmentCommand(
          user = testUserDetails,
          assessmentType = "TEST",
          formVersion = "1",
          identifiers = mapOf(
            IdentifierType.CRN to testCrn,
          ),
        ),
      ).commands[0].result,
    ).assessmentUuid

    command(
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid,
        added = mapOf("foo" to SingleValue("foo_value")),
        removed = listOf(),
        timeline = Timeline(
          type = "SIGNIFICANT_EVENT_A",
          data = mapOf("foo" to "bar"),
        ),
      ),
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid,
        added = mapOf("bar" to SingleValue("bar_value")),
        removed = listOf(),
        timeline = Timeline(
          type = "SIGNIFICANT_EVENT_B",
          data = mapOf("bar" to "baz"),
        ),
      ),
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid,
        added = mapOf("baz" to SingleValue("baz_value")),
        removed = listOf(),
        timeline = Timeline(
          type = "SIGNIFICANT_EVENT_C",
          data = mapOf("baz" to "foo"),
        ),
      ),
    )

    transactionTemplate.execute {
      // backdate timeline entries
      val sql = """
      WITH ordered AS (
          SELECT
              id,
              created_at,
              ROW_NUMBER() OVER (PARTITION BY assessment_uuid ORDER BY id) AS rn,
              COUNT(*) OVER (PARTITION BY assessment_uuid) AS total_count
          FROM timeline
          WHERE assessment_uuid = :assessment
      )
      UPDATE timeline t
      SET created_at = NOW() - INTERVAL '1 day' * (o.total_count - o.rn)
      FROM ordered o
      WHERE t.id = o.id;
      """.trimIndent()
      val query = entityManager.createNativeQuery(sql)
      query.setParameter("assessment", assessmentUuid)

      val rowsUpdated = query.executeUpdate()
      assertEquals(5, rowsUpdated)
    }

    // an additional update on the same day should not create a new daily version:
    command(
      UpdateAssessmentAnswersCommand(
        user = testUserDetails,
        assessmentUuid = assessmentUuid,
        added = mapOf("test" to SingleValue("val")),
        removed = listOf(),
        timeline = Timeline(
          type = "SIGNIFICANT_EVENT_D",
          data = emptyMap(),
        ),
      ),
    )
  }

  @Test
  fun `returns 400 when no identifiers are provided`() {
    query(
      TimelineQuery(
        user = testUserDetails,
      ),
    ).expectStatus().isBadRequest
  }

  @Test
  fun `returns 404 when no user found for the provided user details`() {
    query(
      TimelineQuery(
        user = testUserDetails,
        subject = UserDetails(id = UUID.randomUUID().toString(), name = "Some random user"),
      ),
    ).expectStatus().isNotFound
  }

  fun identifierProvider() = listOf(
    UuidIdentifier(assessmentUuid),
    ExternalIdentifier(
      identifierType = IdentifierType.CRN,
      identifier = testCrn,
      assessmentType = "TEST",
    ),
  )

  @ParameterizedTest
  @MethodSource("identifierProvider")
  fun `returns the daily versions for an assessment`(identifier: AssessmentIdentifier) {
    query(
      DailyVersionsQuery(
        user = testUserDetails,
        assessmentIdentifier = identifier,
      ),
    ).expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .consumeWith { response ->
        val result = assertIs<DailyVersionsQueryResult>(response.responseBody?.queries?.first()?.result)
        result.run {
          assertEquals(5, versions.size)
          assertEquals(5, versions.map { it.createdAt }.toSet().size)
          assertEquals(5, versions.map { it.updatedAt }.toSet().size)
          assertEquals(5, versions.map { it.lastTimelineItemUuid }.toSet().size)

          val timeline = timelineRepository.findByUuid(versions.maxBy { it.createdAt }.lastTimelineItemUuid)
          assertEquals("SIGNIFICANT_EVENT_D", timeline?.customType)
        }
      }
  }

  @Test
  fun `returns 404 when no assessment found for an assessment UUID`() {
    query(
      DailyVersionsQuery(
        user = testUserDetails,
        assessmentIdentifier = UuidIdentifier(UUID.randomUUID()),
      ),
    ).expectStatus().isNotFound
  }

  @Test
  fun `returns 404 when no assessment found for an assessment identifier`() {
    query(
      DailyVersionsQuery(
        user = testUserDetails,
        assessmentIdentifier = ExternalIdentifier(
          identifierType = IdentifierType.CRN,
          identifier = "SOME_RANDOM_CRN",
          assessmentType = "TEST",
        ),
      ),
    ).expectStatus().isNotFound
  }
}
