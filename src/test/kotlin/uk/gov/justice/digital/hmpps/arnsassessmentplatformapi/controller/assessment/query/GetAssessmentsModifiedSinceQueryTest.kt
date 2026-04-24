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
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.GetAssessmentsModifiedSinceQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.GetAssessmentsModifiedSinceQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAssessmentsModifiedSinceQueryTest(
  @Autowired private val assessmentRepository: AssessmentRepository,
  @Autowired private val eventRepository: EventRepository,
  @Autowired private val userDetailsRepository: UserDetailsRepository,
) : IntegrationTestBase() {

  @Test
  fun `it returns assessments modified since the given timestamp`() {
    userDetailsRepository.save(testUserDetailsEntity)
    val type = "MODIFIED_SINCE_${UUID.randomUUID()}"

    val cutoff = LocalDateTime.parse("2025-06-01T12:00:00")

    createAssessmentWithEvent(type, LocalDateTime.parse("2025-05-01T10:00:00"))
    val newAssessment1 = createAssessmentWithEvent(type, LocalDateTime.parse("2025-06-02T10:00:00"))
    val newAssessment2 = createAssessmentWithEvent(type, LocalDateTime.parse("2025-06-03T10:00:00"))

    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsModifiedSinceQuery(
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
    val result = assertIs<GetAssessmentsModifiedSinceQueryResult>(response?.queries?.get(0)?.result)
    assertThat(result.assessments).hasSize(2)
    assertThat(result.assessments.map { it.assessmentUuid })
      .containsExactlyInAnyOrder(newAssessment1.uuid, newAssessment2.uuid)
    assertThat(result.nextCursor).isNull()
  }

  @Test
  fun `cursor paging walks all assessments across pages`() {
    userDetailsRepository.save(testUserDetailsEntity)
    val type = "PAGINATE_${UUID.randomUUID()}"

    val since = LocalDateTime.parse("2025-01-01T00:00:00")

    val created = (1..5).map {
      createAssessmentWithEvent(type, LocalDateTime.parse("2025-06-0${it}T10:00:00"))
    }

    val allAssessmentUuids = mutableSetOf<UUID>()
    var cursor: UUID? = null

    do {
      val request = QueriesRequest(
        queries = listOf(
          GetAssessmentsModifiedSinceQuery(
            user = testUserDetails,
            assessmentType = type,
            since = since,
            after = cursor,
            limit = 2,
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

      val result = assertIs<GetAssessmentsModifiedSinceQueryResult>(response?.queries?.get(0)?.result)
      allAssessmentUuids.addAll(result.assessments.map { it.assessmentUuid })
      cursor = result.nextCursor
    } while (cursor != null)

    assertThat(allAssessmentUuids).containsExactlyInAnyOrderElementsOf(created.map { it.uuid })
  }

  @Test
  fun `it returns empty list when no assessments modified since timestamp`() {
    userDetailsRepository.save(testUserDetailsEntity)
    val type = "EMPTY_${UUID.randomUUID()}"

    createAssessmentWithEvent(type, LocalDateTime.parse("2025-01-01T10:00:00"))

    val request = QueriesRequest(
      queries = listOf(
        GetAssessmentsModifiedSinceQuery(
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

    val result = assertIs<GetAssessmentsModifiedSinceQueryResult>(response?.queries?.get(0)?.result)
    assertThat(result.assessments).isEmpty()
    assertThat(result.nextCursor).isNull()
  }

  private fun createAssessmentWithEvent(type: String, eventCreatedAt: LocalDateTime): AssessmentEntity {
    val assessment = AssessmentEntity(type = type, createdAt = eventCreatedAt)
      .run(assessmentRepository::save)

    listOf(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = eventCreatedAt,
        data = AssessmentCreatedEvent(formVersion = "1", properties = emptyMap()),
        position = 0,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = eventCreatedAt,
        data = FormVersionUpdatedEvent(version = "1"),
        position = 1,
      ),
    ).run(eventRepository::saveAll)

    return assessment
  }
}
