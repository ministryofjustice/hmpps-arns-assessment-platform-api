package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.UserDetailsRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsSoftDeletedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsSoftDeletedSinceQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAssessmentsSoftDeletedSinceQueryTest(
  @Autowired private val assessmentRepository: AssessmentRepository,
  @Autowired private val eventRepository: EventRepository,
  @Autowired private val userDetailsRepository: UserDetailsRepository,
) : IntegrationTestBase() {

  @Test
  fun `it returns assessments soft deleted since the given timestamp`() {
    userDetailsRepository.save(testUserDetailsEntity)
    val type = "SOFT_DELETED_SINCE_${UUID.randomUUID()}"

    val cutoff = LocalDateTime.parse("2025-06-01T12:00:00")

    createSoftDeletedAssessment(type, LocalDateTime.parse("2025-05-01T10:00:00"))
    val deletedAfter1 = createSoftDeletedAssessment(type, LocalDateTime.parse("2025-06-02T10:00:00"))
    val deletedAfter2 = createSoftDeletedAssessment(type, LocalDateTime.parse("2025-06-03T10:00:00"))
    createSoftDeletedAssessment("OTHER_${UUID.randomUUID()}", LocalDateTime.parse("2025-06-04T10:00:00"))

    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsSoftDeletedSinceQuery(
          user = testUserDetails,
          assessmentType = type,
          since = cutoff,
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
      .containsExactlyInAnyOrder(deletedAfter1.uuid, deletedAfter2.uuid)
  }

  @Test
  fun `it returns empty list when no assessments were soft deleted since timestamp`() {
    userDetailsRepository.save(testUserDetailsEntity)
    val type = "SOFT_DELETED_EMPTY_${UUID.randomUUID()}"

    createSoftDeletedAssessment(type, LocalDateTime.parse("2025-01-01T10:00:00"))

    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsSoftDeletedSinceQuery(
          user = testUserDetails,
          assessmentType = type,
          since = LocalDateTime.parse("2025-12-01T00:00:00"),
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

  private fun createSoftDeletedAssessment(type: String, eventCreatedAt: LocalDateTime): AssessmentEntity {
    val assessment = AssessmentEntity(type = type, createdAt = eventCreatedAt)
      .run(assessmentRepository::save)

    listOf(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = eventCreatedAt,
        data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
        position = 0,
        deleted = true,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = eventCreatedAt,
        data = FormVersionUpdatedEvent(version = "1"),
        position = 1,
        deleted = true,
      ),
    ).run(eventRepository::saveAll)

    return assessment
  }
}
