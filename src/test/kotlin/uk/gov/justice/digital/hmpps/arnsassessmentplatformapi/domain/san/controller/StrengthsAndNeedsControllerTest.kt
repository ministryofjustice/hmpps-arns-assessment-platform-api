package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.Pathway
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.domain.san.controller.requests.StartNewPathwayRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertIs

class StrengthsAndNeedsControllerTest : IntegrationTestBase() {

  private fun createAssessment(assessmentType: String = "STRENGTHS_AND_NEEDS") = assertIs<CreateAssessmentCommandResult>(
    command(
      CreateAssessmentCommand(
        testUserDetails,
        assessmentType = assessmentType,
        formVersion = "1",
      ),
    ).commands.single().result,
  ).assessmentUuid

  private fun addAnswers(assessmentUuid: UUID) = (
    command(
      UpdateAssessmentAnswersCommand(
        testUserDetails,
        assessmentUuid.toReference(),
        added = mapOf("accommodation_type" to SingleValue("SETTLED"), "drug_use" to SingleValue("NO")),
        removed = emptyList(),
      ),
    )
    )

  private fun fetchAssessment(assessmentUuid: UUID) = assertIs<AssessmentVersionQueryResult>(
    query(
      AssessmentVersionQuery(user = testUserDetails, assessmentIdentifier = UuidIdentifier(assessmentUuid)),
    )
      .expectStatus().isOk
      .expectBody<QueriesResponse>()
      .returnResult().responseBody!!
      .queries.single().result,
  )

  private fun startNewPathway(assessmentUuid: UUID, roles: List<String> = listOf("ROLE_AAP__COORDINATOR_RW"), previousPathway: Pathway = Pathway.COMMUNITY, newPathway: Pathway = Pathway.CUSTODY) = webTestClient.post().uri("/san/start-new-pathway")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = roles))
    .bodyValue(
      StartNewPathwayRequest(
        user = testUserDetails,
        assessmentUuid = assessmentUuid,
        previousPathway = previousPathway,
        newPathway = newPathway,
      ),
    )
    .exchange()

  @Nested
  inner class StartNewPathway {
    @Test
    fun `it should remove all answers from the assessment`() {
      val assessmentUuid = createAssessment()
      addAnswers(assessmentUuid)
      val assessmentBefore = fetchAssessment(assessmentUuid)
      assertThat(assessmentBefore.answers).hasSize(2)

      startNewPathway(assessmentUuid)
        .expectStatus().isOk

      val assessmentAfter = fetchAssessment(assessmentUuid)
      assertThat(assessmentAfter.answers).isEmpty()
    }

    @Test
    fun `it should succeed when the assessment has no answers`() {
      val assessmentUuid = createAssessment()

      startNewPathway(assessmentUuid)
        .expectStatus().isOk

      val assessmentAfter = fetchAssessment(assessmentUuid)
      assertThat(assessmentAfter.answers).isEmpty()
    }

    @Test
    fun `it should return 400 when the assessment is not a strengths and needs assessment`() {
      val assessmentUuid = createAssessment(assessmentType = "SENTENCE_PLAN")

      startNewPathway(assessmentUuid)
        .expectStatus().isBadRequest
    }

    @Test
    fun `it should return 404 when the assessment does not exist`() {
      startNewPathway(UUID.randomUUID())
        .expectStatus().isNotFound
    }

    @Test
    fun `it should return 400 when the pathway is invalid`() {
      val assessmentUuid = createAssessment()
      addAnswers(assessmentUuid)
      assertThat(fetchAssessment(assessmentUuid).answers).hasSize(2)
      val requestBody = """
        {
          "user": { "id": "test-user", "name": "Test User" },
          "assessmentUuid": "$assessmentUuid",
          "previousPathway": "COMMUNITY",
          "newPathway": "INVALID_PATHWAY"
        }
      """.trimIndent()

      webTestClient.post().uri("/san/start-new-pathway")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AAP__COORDINATOR_RW")))
        .bodyValue(requestBody)
        .exchange().expectStatus().isBadRequest

      assertThat(fetchAssessment(assessmentUuid).answers).hasSize(2)
    }

    @Test
    fun `it should return 400 and keep the answers when the pathway has not changed`() {
      val assessmentUuid = createAssessment()
      addAnswers(assessmentUuid)

      startNewPathway(assessmentUuid, previousPathway = Pathway.COMMUNITY, newPathway = Pathway.COMMUNITY)
        .expectStatus().isBadRequest

      assertThat(fetchAssessment(assessmentUuid).answers).hasSize(2)
    }
  }

  @Nested
  inner class StartNewPathwayAuthorisation {
    @ParameterizedTest
    @ValueSource(strings = ["ROLE_AAP__FRONTEND_RW", "ROLE_AAP__COORDINATOR_RW"])
    fun `it should succeed when the user has the correct role`(role: String) {
      val assessmentUuid = createAssessment()

      startNewPathway(assessmentUuid, roles = listOf(role))
        .expectStatus().isOk
    }

    @Test
    fun `it should return 403 when the user does not have the correct role`() {
      val assessmentUuid = createAssessment()

      startNewPathway(assessmentUuid, roles = listOf("ROLE_AAP__FRONTEND_RO"))
        .expectStatus().isForbidden
    }

    @Test
    fun `it should return 403 when the user has no roles`() {
      val assessmentUuid = createAssessment()

      startNewPathway(assessmentUuid, roles = emptyList())
        .expectStatus().isForbidden
    }

    @Test
    fun `it should return 403 when the user has an unrelated role`() {
      val assessmentUuid = createAssessment()

      startNewPathway(assessmentUuid, roles = listOf("ROLE_AAP__COORDINATOR_RO"))
        .expectStatus().isForbidden
    }
  }
}
