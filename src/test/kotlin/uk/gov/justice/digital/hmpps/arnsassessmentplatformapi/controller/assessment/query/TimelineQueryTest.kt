package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.TimelineQueryResult
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineQueryTest(
  @Autowired
  val entityManager: EntityManager,
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val timelineRepository: TimelineRepository,
  @Autowired
  val userDetailsRepository: UserDetailsRepository,
) : IntegrationTestBase() {
  private val user1 = UserDetails(id = UUID.randomUUID().toString(), name = "Foo User 1")
  private val user2 = UserDetails(id = UUID.randomUUID().toString(), name = "Foo User 2")

  private lateinit var user1Assessment1Uuid: UUID
  private lateinit var user1Assessment2Uuid: UUID
  private lateinit var user2Assessment1Uuid: UUID
  private lateinit var user2Assessment2Uuid: UUID

  private val testCrn =
    UUID.randomUUID().toString() // TODO: If we validate CRN then we may be required to improve this test data

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @BeforeAll
  fun setUp() {
    user1Assessment1Uuid = assertIs<CreateAssessmentCommandResult>(
      command(CreateAssessmentCommand(user = user1, assessmentType = "TEST", formVersion = "1")).commands[0].result,
    ).assessmentUuid
    user1Assessment2Uuid = assertIs<CreateAssessmentCommandResult>(
      command(
        CreateAssessmentCommand(
          user = user1,
          assessmentType = "TEST",
          formVersion = "1",
          identifiers = mapOf(
            IdentifierType.CRN to testCrn,
          ),
        ),
      ).commands[0].result,
    ).assessmentUuid

    user2Assessment1Uuid = assertIs<CreateAssessmentCommandResult>(
      command(CreateAssessmentCommand(user = user2, assessmentType = "TEST", formVersion = "1")).commands[0].result,
    ).assessmentUuid
    user2Assessment2Uuid = assertIs<CreateAssessmentCommandResult>(
      command(CreateAssessmentCommand(user = user2, assessmentType = "TEST", formVersion = "1")).commands[0].result,
    ).assessmentUuid

    listOf(
      Pair(user1, user1Assessment1Uuid),
      Pair(user2, user2Assessment1Uuid),
    ).flatMap { (user, assessmentUuid) ->
      listOf(
        UpdateAssessmentAnswersCommand(
          user = user,
          assessmentUuid = assessmentUuid.toReference(),
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = listOf(),
          timeline = Timeline(
            type = "SIGNIFICANT_EVENT_A",
            data = mapOf("foo" to "bar"),
          ),
        ),
        UpdateAssessmentPropertiesCommand(
          user = user,
          assessmentUuid = assessmentUuid.toReference(),
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = listOf(),
          timeline = Timeline(
            type = "SIGNIFICANT_EVENT_B",
            data = mapOf("bar" to "baz"),
          ),
        ),
      )
    }.let { command(*it.toTypedArray()) }

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
          WHERE assessment_uuid IN (:user1assessment, :user2assessment)
      )
      UPDATE timeline t
      SET created_at = NOW() - INTERVAL '1 day' * (o.total_count - o.rn)
      FROM ordered o
      WHERE t.id = o.id;
      """.trimIndent()
      val query = entityManager.createNativeQuery(sql)
      query.setParameter("user1assessment", user1Assessment1Uuid)
      query.setParameter("user2assessment", user2Assessment1Uuid)

      val rowsUpdated = query.executeUpdate()
      assertEquals(8, rowsUpdated)
    }
  }

  @Test
  fun `returns 400 when no identifiers are provided`() {
    query(
      TimelineQuery(
        user = testUserDetails,
      ),
    ).expectStatus().isBadRequest
  }

  @ParameterizedTest
  @EnumSource(AuthSource::class, names = ["OASYS", "HMPPS_AUTH", "NOT_SPECIFIED"])
  fun `returns the stored timeline actor identity rather than the requesting user`(authSource: AuthSource) {
    val actorUuid = UUID.randomUUID()
    val actor = userDetailsRepository.save(
      UserDetailsEntity(
        uuid = actorUuid,
        userId = "OASYS_ACTOR_$actorUuid",
        displayName = "Timeline Actor",
        authSource = authSource,
      ),
    )
    val assessment = assessmentRepository.save(
      AssessmentEntity(type = "TEST", createdAt = clock.now().minusMinutes(1)),
    )
    timelineRepository.save(
      TimelineEntity(
        position = 1,
        createdAt = assessment.createdAt,
        user = actor,
        assessment = assessment,
        eventType = "AssessmentCreatedEvent",
      ),
    )

    query(
      TimelineQuery(
        user = testUserDetails,
        assessmentIdentifier = UuidIdentifier(assessment.uuid),
      ),
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.queries[0].result.timeline.length()").isEqualTo(1)
      .jsonPath("$.queries[0].result.timeline[0].user.id").isEqualTo(actorUuid.toString())
      .jsonPath("$.queries[0].result.timeline[0].user.name").isEqualTo("Timeline Actor")
      .jsonPath("$.queries[0].result.timeline[0].user.userId").isEqualTo("OASYS_ACTOR_$actorUuid")
      .jsonPath("$.queries[0].result.timeline[0].user.authSource").isEqualTo(authSource.name)
  }

  @Test
  fun `returns the timeline for a user`() {
    query(
      TimelineQuery(
        user = testUserDetails,
        subject = user1,
      ),
    ).expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .consumeWith { response ->
        val result = assertIs<TimelineQueryResult>(response.responseBody?.queries?.first()?.result)
        result.run {
          assertEquals(6, timeline.size)
          assertThat(timeline.all { t -> t.user == user1 })
          assertEquals(setOf(user1Assessment1Uuid, user1Assessment2Uuid), timeline.map { it.assessment }.toSet())
        }
      }
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

  @Test
  fun `returns the timeline for an assessment UUID`() {
    query(
      TimelineQuery(
        user = testUserDetails,
        assessmentIdentifier = UuidIdentifier(user1Assessment1Uuid),
      ),
    ).expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .consumeWith { response ->
        val result = assertIs<TimelineQueryResult>(response.responseBody?.queries?.first()?.result)
        result.run {
          assertEquals(4, timeline.size)
          assertEquals(
            setOf(user1Assessment1Uuid),
            timeline.map { it.assessment }.toSet(),
          )
        }
      }
  }

  @Test
  fun `returns 404 when no assessment found for an assessment UUID`() {
    query(
      TimelineQuery(
        user = testUserDetails,
        assessmentIdentifier = UuidIdentifier(UUID.randomUUID()),
      ),
    ).expectStatus().isNotFound
  }

  @Test
  fun `returns the timeline for an assessment identifier`() {
    query(
      TimelineQuery(
        user = testUserDetails,
        assessmentIdentifier = ExternalIdentifier(
          identifierType = IdentifierType.CRN,
          identifier = testCrn,
          assessmentType = "TEST",
        ),
      ),
    ).expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .consumeWith { response ->
        val result = assertIs<TimelineQueryResult>(response.responseBody?.queries?.first()?.result)
        result.run {
          assertEquals(2, timeline.size)
          assertEquals(setOf(user1Assessment2Uuid), timeline.map { it.assessment }.toSet())
        }
      }
  }

  @Test
  fun `returns 404 when no assessment found for an assessment identifier`() {
    query(
      TimelineQuery(
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
