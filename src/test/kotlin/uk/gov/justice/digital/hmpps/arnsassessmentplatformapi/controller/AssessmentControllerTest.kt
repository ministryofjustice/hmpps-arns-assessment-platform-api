package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBusFactory
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.util.UUID
import kotlin.test.assertIs

class AssessmentControllerTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
  @Autowired
  private val commandBusFactory: CommandBusFactory,
) : IntegrationTestBase() {
  val commandBus = commandBusFactory.create()

  private fun createAssessment() = assertIs<CreateAssessmentCommandResult>(
    command(CreateAssessmentCommand(testUserDetails, assessmentType = "TEST", formVersion = "1")).commands.single().result,
  ).assessmentUuid

  private fun latestAggregate(assessmentUuid: UUID) = aggregateRepository.findTopByAssessmentUuidAndDataTypeOrderByPositionDesc(
    assessmentUuid,
    AssessmentAggregate::class.simpleName!!,
  )

  private fun rebuild(assessmentUuid: UUID, roles: List<String> = listOf("ROLE_AAP__COORDINATOR_RW")) = webTestClient.post().uri("/assessment/$assessmentUuid/rebuild")
    .headers(setAuthorisation(roles = roles))
    .exchange()

  @Nested
  inner class Command {
    @Test
    fun `it returns 400 when the command does not exist`() {
      val request = """
      {
        "commands": [
          {
            "type": "UnknownCommand",
            "user": {
              "id": "test-user",
              "name": "Test User"
            },
            "assessmentUuid": "${UUID.randomUUID()}"
          }
        ]
      }
      """.trimIndent()

      val response = webTestClient.post().uri("/command")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(String::class.java)
        .returnResult()
        .responseBody

      assertThat(response).contains("Invalid payload: JSON parse error: Could not resolve type id 'UnknownCommand'")
    }

    @Test
    fun `it can process multiple commands`() {
      val request = CommandsRequest(
        commands = listOf(
          CreateAssessmentCommand(
            UserDetails("test-user-1", "Test User"),
            assessmentType = "TEST",
            formVersion = "1",
          ),
          CreateAssessmentCommand(
            UserDetails("test-user-2", "Test User"),
            assessmentType = "TEST",
            formVersion = "1",
          ),
        ),
      )

      val response = webTestClient.post().uri("/command")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk
        .expectBody(CommandsResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response?.commands).hasSize(2)
      assertThat(response?.commands[0]?.request).isEqualTo(request.commands[0])
      assertThat(response?.commands[1]?.request).isEqualTo(request.commands[1])

      val result1 = assertIs<CreateAssessmentCommandResult>(response?.commands[0]?.result)
      val result2 = assertIs<CreateAssessmentCommandResult>(response?.commands[1]?.result)

      assertThat(result1.assessmentUuid).isNotEqualTo(result2.assessmentUuid)

      assertThat(assessmentRepository.findByUuid(result1.assessmentUuid)).isNotNull()
      assertThat(assessmentRepository.findByUuid(result2.assessmentUuid)).isNotNull()
    }
  }

  @Nested
  inner class Query {
    @Test
    fun `it returns 400 when the query does not exist`() {
      val request = """
        {
          "queries": [
            {
              "type": "UnknownQuery",
              "user": {
                "id": "test-user",
                "name": "Test User"
              },
              "assessmentUuid": "${UUID.randomUUID()}"
            }
          ]
        }
      """.trimIndent()

      val response = webTestClient.post().uri("/query")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(String::class.java)
        .returnResult()
        .responseBody

      assertThat(response).contains("Invalid payload: JSON parse error: Could not resolve type id 'UnknownQuery'")
    }

    @Test
    fun `it can process multiple queries`() {
      val assessment1 = CreateAssessmentCommand(
        UserDetails("test-user-1", "Test User"),
        assessmentType = "TEST",
        formVersion = "1",
      )
      val assessment2 = CreateAssessmentCommand(
        UserDetails("test-user-2", "Test User"),
        assessmentType = "TEST",
        formVersion = "1",
      )

      val httpRequest = MockHttpServletRequest()
      RequestContextHolder.setRequestAttributes(ServletRequestAttributes(httpRequest))

      try {
        commandBus.dispatchAndPersist(listOf(assessment1))
        commandBus.dispatchAndPersist(listOf(assessment2))
      } finally {
        RequestContextHolder.resetRequestAttributes()
      }

      val request = QueriesRequest(
        queries = listOf(
          AssessmentVersionQuery(
            user = UserDetails("test-user-1", "Test User"),
            assessmentIdentifier = UuidIdentifier(assessment1.assessmentUuid.value),
          ),
          AssessmentVersionQuery(
            user = UserDetails("test-user-2", "Test User"),
            assessmentIdentifier = UuidIdentifier(assessment2.assessmentUuid.value),
          ),
        ),
      )

      val response = webTestClient.post().uri("/query")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response?.queries).hasSize(2)
      assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
      assertThat(response?.queries[1]?.request).isEqualTo(request.queries[1])

      assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)
      assertIs<AssessmentVersionQueryResult>(response?.queries[1]?.result)

      listOf(assessment1, assessment2).forEach { assessment ->
        aggregateRepository.findTopByAssessmentUuidAndDataTypeAndEventsToLessThanEqualOrderByPositionDesc(
          assessment.assessmentUuid.value,
          AssessmentAggregate::class.simpleName!!,
          clock.now(),
        ).let { assertThat(it).isNotNull() }
      }
    }
  }

  @Nested
  inner class RebuildAggregates {
    @Test
    fun `it replaces existing aggregates with ones rebuilt from events`() {
      val assessmentUuid = createAssessment()

      command(
        UpdateAssessmentAnswersCommand(
          user = testUserDetails,
          assessmentUuid = assessmentUuid.toReference(),
          added = mapOf("q1" to SingleValue("a1"), "q2" to SingleValue("a2")),
          removed = emptyList(),
        ),
        UpdateAssessmentAnswersCommand(
          user = testUserDetails,
          assessmentUuid = assessmentUuid.toReference(),
          added = mapOf("q3" to SingleValue("a3")),
          removed = listOf("q2"),
        ),
      )

      val originalAggregate = latestAggregate(assessmentUuid)!!
      val originalData = originalAggregate.data as AssessmentAggregate

      rebuild(assessmentUuid).expectStatus().isOk

      val rebuiltAggregate = latestAggregate(assessmentUuid)!!
      val rebuiltData = rebuiltAggregate.data as AssessmentAggregate

      assertThat(aggregateRepository.findAll().map { it.uuid }).doesNotContain(originalAggregate.uuid)
      assertThat(rebuiltAggregate.uuid).isNotEqualTo(originalAggregate.uuid)
      assertThat(rebuiltData).isEqualTo(originalData)
      assertThat(rebuiltData.answers).isEqualTo(mapOf("q1" to SingleValue("a1"), "q3" to SingleValue("a3")))
    }

    @Test
    fun `the assessment can still be queried after a rebuild`() {
      val assessmentUuid = createAssessment()

      command(
        UpdateAssessmentAnswersCommand(
          user = testUserDetails,
          assessmentUuid = assessmentUuid.toReference(),
          added = mapOf("q1" to SingleValue("a1")),
          removed = emptyList(),
        ),
      )

      rebuild(assessmentUuid).expectStatus().isOk

      val response = query(AssessmentVersionQuery(testUserDetails, UuidIdentifier(assessmentUuid)))
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody

      val result = assertIs<AssessmentVersionQueryResult>(response?.queries?.single()?.result)
      assertThat(result.answers).isEqualTo(mapOf("q1" to SingleValue("a1")))
    }

    @Test
    fun `it returns 404 when the assessment does not exist`() {
      rebuild(UUID.randomUUID()).expectStatus().isNotFound
    }
  }

  @Nested
  inner class Security {
    @Nested
    inner class CommandEndpoint {
      @Test
      fun `it allows access with ROLE_AAP__FRONTEND_RW`() {
        val request = CommandsRequest(
          commands = listOf(
            CreateAssessmentCommand(
              user = UserDetails("test-user", "Test User"),
              assessmentType = "TEST",
              formVersion = "1",
            ),
          ),
        )

        webTestClient.post().uri("/command")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
          .bodyValue(request)
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `it denies access with no roles`() {
        val request = CommandsRequest(
          commands = listOf(
            CreateAssessmentCommand(
              user = UserDetails("test-user", "Test User"),
              assessmentType = "TEST",
              formVersion = "1",
            ),
          ),
        )

        webTestClient.post().uri("/command")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(request)
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class QueryEndpoint {
      @Test
      fun `it allows access with ROLE_AAP__FRONTEND_RW`() {
        val assessment = CreateAssessmentCommand(
          user = UserDetails("test-user", "Test User"),
          assessmentType = "TEST",
          formVersion = "1",
        )

        val httpRequest = MockHttpServletRequest()
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(httpRequest))

        try {
          commandBus.dispatchAndPersist(listOf(assessment))
        } finally {
          RequestContextHolder.resetRequestAttributes()
        }

        val request = QueriesRequest(
          queries = listOf(
            AssessmentVersionQuery(
              user = UserDetails("test-user", "Test User"),
              assessmentIdentifier = UuidIdentifier(assessment.assessmentUuid.value),
            ),
          ),
        )

        webTestClient.post().uri("/query")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
          .bodyValue(request)
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `it denies access with no roles`() {
        val request = QueriesRequest(
          queries = listOf(
            AssessmentVersionQuery(
              user = UserDetails("test-user", "Test User"),
              assessmentIdentifier = UuidIdentifier(UUID.randomUUID()),
            ),
          ),
        )

        webTestClient.post().uri("/query")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(request)
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class RebuildEndpoint {
      @ParameterizedTest
      @ValueSource(strings = ["ROLE_AAP__COORDINATOR_RW", "ROLE_SENTENCE_PLAN_WRITE", "ROLE_AAP_DATA_DELETION"])
      fun `it allows access with permitted roles`(role: String) {
        rebuild(createAssessment(), roles = listOf(role)).expectStatus().isOk
      }

      @Test
      fun `it denies access with no roles`() {
        rebuild(UUID.randomUUID(), roles = listOf()).expectStatus().isForbidden
      }

      @Test
      fun `it denies access with an unrelated role`() {
        rebuild(UUID.randomUUID(), roles = listOf("ROLE_AAP__FRONTEND_RW")).expectStatus().isForbidden
      }
    }
  }
}
