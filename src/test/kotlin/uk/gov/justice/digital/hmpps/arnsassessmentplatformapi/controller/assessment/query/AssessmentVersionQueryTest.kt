package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.CollectionRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.CollectionEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class AssessmentVersionQueryTest(
  @Autowired
  private val collectionRepository: CollectionRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
  @Autowired
  private val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @Test
  fun `it fetches the latest aggregate for an assessment`() {
    val assessment: CollectionEntity = CollectionEntity().run(collectionRepository::save)

    val events = listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent("1"),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
      ),
    ).run(eventRepository::saveAll)

    val aggregateData = AssessmentVersionAggregate(
      answers = mutableMapOf("foo" to listOf("foo_value")),
      deletedAnswers = mutableMapOf(),
      collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
      formVersion = "1",
    ).apply { numberOfEventsApplied = events.size.toLong() }

    AggregateEntity(
      collection = assessment,
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
      data = aggregateData,
    ).run(aggregateRepository::save)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          collectionUuid = assessment.uuid,
        ),
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

    assertThat(response?.queries).hasSize(1)
    assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    assertThat(result.answers).isEqualTo(aggregateData.getAnswers())
    assertThat(result.collaborators).isEqualTo(aggregateData.getCollaborators())
    assertThat(result.formVersion).isEqualTo(aggregateData.getFormVersion())
  }

  @Test
  fun `it fetches an aggregate for a point in time`() {
    val assessment: CollectionEntity = CollectionEntity().run(collectionRepository::save)

    listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent("1"),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("updated_foo_value")),
          removed = emptyList(),
        ),
      ),
    ).run(eventRepository::saveAll)

    val firstAggregateData = AssessmentVersionAggregate(
      answers = mutableMapOf("foo" to listOf("foo_value")),
      deletedAnswers = mutableMapOf(),
      collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
      formVersion = "1",
    ).apply { numberOfEventsApplied = 1 }

    val secondAggregateData = AssessmentVersionAggregate(
      answers = mutableMapOf("foo" to listOf("updated_foo_value")),
      deletedAnswers = mutableMapOf(),
      collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
      formVersion = "1",
    ).apply { numberOfEventsApplied = 2 }

    listOf(
      AggregateEntity(
        collection = assessment,
        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = secondAggregateData,
      ),
      AggregateEntity(
        collection = assessment,
        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = firstAggregateData,
      ),
    ).run(aggregateRepository::saveAll)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          collectionUuid = assessment.uuid,
          timestamp = LocalDateTime.parse("2025-01-01T12:15:00"),
        ),
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

    assertThat(response?.queries).hasSize(1)
    assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    assertThat(result.answers).isEqualTo(firstAggregateData.getAnswers())
    assertThat(result.collaborators).isEqualTo(firstAggregateData.getCollaborators())
    assertThat(result.formVersion).isEqualTo(firstAggregateData.getFormVersion())
  }

  @Test
  fun `it creates an aggregate for an assessment where none exists`() {
    val assessment: CollectionEntity = CollectionEntity(
      createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    ).run(collectionRepository::save)

    listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent("1"),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
      ),
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        collection = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AnswersUpdatedEvent(
          added = mapOf("foo" to listOf("updated_foo_value")),
          removed = emptyList(),
        ),
      ),
    ).run(eventRepository::saveAll)

    val aggregateData = AssessmentVersionAggregate(
      answers = mutableMapOf("foo" to listOf("updated_foo_value")),
      deletedAnswers = mutableMapOf(),
      collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
      formVersion = "1",
    )

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          collectionUuid = assessment.uuid,
        ),
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

    assertThat(response?.queries).hasSize(1)
    assertThat(response?.queries[0]?.request).isEqualTo(request.queries[0])
    val result = assertIs<AssessmentVersionQueryResult>(response?.queries[0]?.result)

    assertThat(result.answers).isEqualTo(aggregateData.getAnswers())
    assertThat(result.collaborators).isEqualTo(aggregateData.getCollaborators())
    assertThat(result.formVersion).isEqualTo(aggregateData.getFormVersion())
  }

  @Test
  fun `it returns 404 when there is no assessments`() {
    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = User("test-user", "Test User"),
          collectionUuid = UUID.randomUUID(),
        ),
      ),
    )

    webTestClient.post().uri("/query")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isNotFound
  }
}
