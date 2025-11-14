package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class AssessmentVersionQueryTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
  @Autowired
  private val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @Test
  fun `it fetches the latest aggregate for an assessment`() {
    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)

    val events = listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = emptyMap(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1", timeline = null),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    ).run(eventRepository::saveAll)

    val aggregateData = AssessmentAggregate().apply {
      answers.put("foo", listOf("foo_value"))
      collaborators.add(User("FOO_USER", "Foo User"))
      formVersion = "1"
    }

    AggregateEntity(
      assessment = assessment,
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
      data = aggregateData,
    )
      .apply { numberOfEventsApplied = events.size.toLong() }
      .run(aggregateRepository::save)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          assessmentUuid = assessment.uuid,
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
    assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    assertThat(result.answers).isEqualTo(aggregateData.answers)
    assertThat(result.collaborators).isEqualTo(aggregateData.collaborators)
    assertThat(result.formVersion).isEqualTo(aggregateData.formVersion)
  }

  @Test
  fun `it fetches an aggregate for a point in time`() {
    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)

    listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = mapOf(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1", timeline = null),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to listOf("updated_foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    ).run(eventRepository::saveAll)

    val firstAggregateData = AssessmentAggregate().apply {
      answers.put("foo", listOf("foo_value"))
      collaborators.add(User("FOO_USER", "Foo User"))
      formVersion = "1"
    }

    val secondAggregateData = AssessmentAggregate().apply {
      answers.put("foo", listOf("updated_foo_value"))
      collaborators.add(User("FOO_USER", "Foo User"))
      formVersion = "1"
    }

    listOf(
      AggregateEntity(
        assessment = assessment,
        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = secondAggregateData,
      ).apply { numberOfEventsApplied = 2 },
      AggregateEntity(
        assessment = assessment,
        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = firstAggregateData,
      ).apply { numberOfEventsApplied = 1 },
    ).run(aggregateRepository::saveAll)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          assessmentUuid = assessment.uuid,
          timestamp = LocalDateTime.parse("2025-01-01T12:15:00"),
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
    assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    assertThat(result.answers).isEqualTo(firstAggregateData.answers)
    assertThat(result.collaborators).isEqualTo(firstAggregateData.collaborators)
    assertThat(result.formVersion).isEqualTo(firstAggregateData.formVersion)
  }

  @Test
  fun `it creates an aggregate for an assessment where none exists`() {
    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)

    listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = mapOf(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1", timeline = null),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to listOf("updated_foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    ).run(eventRepository::saveAll)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          assessmentUuid = assessment.uuid,
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
    assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    val expectedAggregate = AssessmentAggregate().apply {
      answers.put("foo", listOf("updated_foo_value"))
      collaborators.add(User("FOO_USER", "Foo User"))
      formVersion = "1"
    }

    assertThat(result.answers).isEqualTo(expectedAggregate.answers)
    assertThat(result.collaborators).isEqualTo(expectedAggregate.collaborators)
    assertThat(result.formVersion).isEqualTo(expectedAggregate.formVersion)

    val persistedAggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessment.uuid,
      AssessmentAggregate::class.simpleName!!,
      Clock.now(),
    )

    assertThat(persistedAggregate).isNull()
  }

  @Test
  fun `it returns 404 when there is no assessments`() {
    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          assessmentUuid = UUID.randomUUID(),
        ),
      ),
    )

    webTestClient.post().uri("/query")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isNotFound
  }
}
