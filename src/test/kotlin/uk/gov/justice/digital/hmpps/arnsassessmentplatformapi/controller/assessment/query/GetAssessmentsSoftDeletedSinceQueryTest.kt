package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsSoftDeletedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsSoftDeletedSinceQueryResult
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAssessmentsSoftDeletedSinceQueryTest(
  @Autowired private val userDetailsRepository: UserDetailsRepository,
) : IntegrationTestBase() {

  private val type = "SENTENCE_PLAN"

  @Test
  fun `it returns assessments soft deleted since the given timestamp`() {
    val since = LocalDateTime.now().minusMinutes(1)

    val deletedAfter1 = createSoftDeletedAssessment(type, LocalDateTime.now())
    val deletedAfter2 = createSoftDeletedAssessment(type, LocalDateTime.now())

    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsSoftDeletedSinceQuery(
          user = testUserDetails,
          assessmentType = type,
          since = since,
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

    assertThat(response?.queries).hasSize(1)
    val result = assertIs<GetAssessmentsSoftDeletedSinceQueryResult>(response?.queries?.get(0)?.result)
    assertThat(result.assessments)
      .contains(deletedAfter1, deletedAfter2)
  }

  @Test
  fun `it returns empty list when no assessments were soft deleted since timestamp`() {
    createSoftDeletedAssessment(type, LocalDateTime.now())

    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsSoftDeletedSinceQuery(
          user = testUserDetails,
          assessmentType = type,
          since = LocalDateTime.now().plusMinutes(1),
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

    val result = assertIs<GetAssessmentsSoftDeletedSinceQueryResult>(response?.queries?.get(0)?.result)
    assertThat(result.assessments).isEmpty()
  }

  private fun createSoftDeletedAssessment(type: String, pointInTime: LocalDateTime): UUID {
    val response = webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(
        CommandsRequest(
          commands = listOf(
            CreateAssessmentCommand(
              user = testUserDetails,
              formVersion = "1",
              assessmentType = type,
              properties = mapOf("foo" to SingleValue("bar")),
              flags = listOf("SAN_BETA"),
              identifiers = mapOf(
                IdentifierType.CRN to "CRN123",
              ),
              timeline = Timeline(
                type = "test",
                data = mapOf("bar" to listOf("baz")),
              ),
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody!!

    val result = assertIs<CreateAssessmentCommandResult>(response.commands[0].result)

    webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(
        CommandsRequest(
          commands = listOf(
            SoftDeleteCommand(
              user = testUserDetails,
              assessmentUuid = result.assessmentUuid.toReference(),
              pointInTime = pointInTime,
            ),
          ),
        ),
      ).exchange()
      .expectStatus().isOk

    return result.assessmentUuid
  }
}
