package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.bus.CommandBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.bus.EventBus
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersion
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class AssessmentControllerTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
) : IntegrationTestBase() {
  @Autowired
  private lateinit var commandBus: CommandBus

  @Autowired
  private lateinit var eventBus: EventBus

  @Nested
  inner class Command {
    @Test
    fun `it returns 400 when the command does not exist`() {
      val request = """
      {
        "commands": [
          {
            "type": "UNKNOWN_COMMAND",
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
        .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(String::class.java)
        .returnResult()
        .responseBody

      assertThat(response).contains("Invalid payload: JSON parse error: Could not resolve type id 'UNKNOWN_COMMAND'")
    }

    @Test
    fun `it can process multiple commands`() {
      val request = CommandsRequest(
        commands = listOf(
          CreateAssessment(User("test-user-1", "Test User")),
          CreateAssessment(User("test-user-2", "Test User")),
        ),
      )

      val response = webTestClient.post().uri("/command")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_WRITE")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk
        .expectBody(CommandsResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response?.commands).hasSize(2)
      assertThat(response?.commands[0]?.request).isEqualTo(request.commands[0])
      assertThat(response?.commands[1]?.request).isEqualTo(request.commands[1])

      val result1 = assertIs<CreateAssessmentResult>(response?.commands[0]?.result)
      val result2 = assertIs<CreateAssessmentResult>(response?.commands[1]?.result)

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
              "type": "UNKNOWN_QUERY",
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
        .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(String::class.java)
        .returnResult()
        .responseBody

      assertThat(response).contains("Invalid payload: JSON parse error: Could not resolve type id 'UNKNOWN_QUERY'")
    }

    @Test
    fun `it can process multiple queries`() {
      val assessment1 = CreateAssessment(User("test-user-1", "Test User"))
      val assessment2 = CreateAssessment(User("test-user-2", "Test User"))

      val httpRequest = MockHttpServletRequest()
      RequestContextHolder.setRequestAttributes(ServletRequestAttributes(httpRequest))

      try {
        commandBus.dispatch(assessment1)
        commandBus.dispatch(assessment2)
        eventBus.commit()
      } finally {
        RequestContextHolder.resetRequestAttributes()
      }

      val request = QueriesRequest(
        queries = listOf(
          AssessmentVersion(User("test-user-1", "Test User"), assessment1.assessmentUuid),
          AssessmentVersion(User("test-user-2", "Test User"), assessment2.assessmentUuid),
        ),
      )

      val response = webTestClient.post().uri("/query")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk
        .expectBody(QueriesResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response?.queries).hasSize(2)
      assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
      assertThat(response?.queries[1]?.request).isEqualTo(request.queries[1])

      assertIs<AssessmentVersionResult>(response?.queries[0]?.result)
      assertIs<AssessmentVersionResult>(response?.queries[1]?.result)

      listOf(assessment1, assessment2).forEach { assessment ->
        aggregateRepository.findByAssessmentAndTypeBeforeDate(
          assessment.assessmentUuid,
          AssessmentVersionAggregate.aggregateType,
          LocalDateTime.now(),
        ).let { assertThat(it).isNotNull() }
      }
    }
  }
}
