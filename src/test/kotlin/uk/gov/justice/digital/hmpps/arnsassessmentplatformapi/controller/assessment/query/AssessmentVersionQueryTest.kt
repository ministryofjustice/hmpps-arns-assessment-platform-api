package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collaborator
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentIdentifierEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AuthSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.IdentifierType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.UserDetailsEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.ExternalIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssessmentVersionQueryTest(
  @Autowired
  private val assessmentRepository: AssessmentRepository,
  @Autowired
  private val aggregateRepository: AggregateRepository,
  @Autowired
  private val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @ParameterizedTest
  @MethodSource("assessmentAndIdentifierProvider")
  fun `it fetches the latest aggregate for an assessment`(assessment: AssessmentEntity, identifier: AssessmentIdentifier) {
    assessmentRepository.save(assessment)

    val events = listOf(
      EventEntity(
        user = UserDetailsEntity(userId = "FOO_USER", displayName = "Foo User", authSource = AuthSource.DELIUS),
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = emptyMap(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1", timeline = null),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    ).run(eventRepository::saveAll)

    val aggregateData = AssessmentAggregate().apply {
      answers.put("foo", SingleValue("foo_value"))
      collaborators.add(Collaborator.from(testUserDetailsEntity))
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
          user = testUserDetails,
          assessmentIdentifier = identifier,
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
    assertThat(result.assessmentType).isEqualTo(assessment.type)
    assertThat(result.formVersion).isEqualTo(aggregateData.formVersion)
    assertThat(result.identifiers).hasSize(1)
    assertThat(result.identifiers).isEqualTo(assessment.identifiersMap())
  }

  @Test
  fun `it fetches an aggregate for a point in time`() {
    val assessment = AssessmentEntity(
      type = "TEST",
      createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    ).run(assessmentRepository::save)

    listOf(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = assessment.createdAt,
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = mapOf(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1", timeline = null),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("updated_foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    ).run(eventRepository::saveAll)

    val firstAggregateData = AssessmentAggregate().apply {
      answers.put("foo", SingleValue("foo_value"))
      collaborators.add(Collaborator.from(testUserDetailsEntity))
      formVersion = "1"
    }

    val secondAggregateData = AssessmentAggregate().apply {
      answers.put("foo", SingleValue("updated_foo_value"))
      collaborators.add(Collaborator.from(testUserDetailsEntity))
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
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessment.uuid),
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
    assertThat(result.assessmentType).isEqualTo(assessment.type)
  }

  @Test
  fun `it creates an aggregate for an assessment where none exists`() {
    val assessment = AssessmentEntity(type = "TEST").run(assessmentRepository::save)

    listOf(
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = AssessmentCreatedEvent(
          formVersion = "1",
          properties = mapOf(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        data = FormVersionUpdatedEvent(version = "1", timeline = null),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("updated_foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
      ),
    ).run(eventRepository::saveAll)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessment.uuid),
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
      answers.put("foo", SingleValue("updated_foo_value"))
      collaborators.add(Collaborator.from(testUserDetailsEntity))
      formVersion = "1"
    }

    assertThat(result.answers).isEqualTo(expectedAggregate.answers)
    assertThat(result.collaborators).isEqualTo(expectedAggregate.collaborators)
    assertThat(result.assessmentType).isEqualTo(assessment.type)
    assertThat(result.formVersion).isEqualTo(expectedAggregate.formVersion)

    val persistedAggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessment.uuid,
      AssessmentAggregate::class.simpleName!!,
      Clock.now(),
    )

    assertThat(persistedAggregate).isNull()
  }

  @Test
  fun `events with nested child events are atomic`() {
    val assessment = AssessmentEntity(
      type = "TEST",
      createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    ).run(assessmentRepository::save)

    val assessmentCreatedEvent = EventEntity(
      user = testUserDetailsEntity,
      assessment = assessment,
      createdAt = assessment.createdAt,
      data = AssessmentCreatedEvent(
        formVersion = "1",
        properties = mapOf(),
        timeline = null,
      ),
    )

    val groupEvent = EventEntity(
      user = testUserDetailsEntity,
      assessment = assessment,
      createdAt = LocalDateTime.parse("2025-01-01T12:10:00"),
      data = GroupEvent(timeline = null),
    )

    listOf(
      assessmentCreatedEvent,
      groupEvent,
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:10:00"),
        data = FormVersionUpdatedEvent(version = "2", timeline = null),
        parent = groupEvent,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:20:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
        parent = groupEvent,
      ),
      EventEntity(
        user = testUserDetailsEntity,
        assessment = assessment,
        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
        data = AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("updated_foo_value")),
          removed = emptyList(),
          timeline = null,
        ),
        parent = groupEvent,
      ),
    ).run(eventRepository::saveAll)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessment.uuid),
          timestamp = LocalDateTime.parse("2025-01-01T12:25:00"),
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
      answers.put("foo", SingleValue("updated_foo_value"))
      collaborators.add(Collaborator.from(testUserDetailsEntity))
      formVersion = "2"
    }

    assertThat(result.answers).isEqualTo(expectedAggregate.answers)
    assertThat(result.collaborators).isEqualTo(expectedAggregate.collaborators)
    assertThat(result.assessmentType).isEqualTo(assessment.type)
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
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(UUID.randomUUID()),
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

  @Test
  fun `it returns an error when the requested point in time is before the assessment created date`() {
    val assessment = AssessmentEntity(
      type = "TEST",
      createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
    ).run(assessmentRepository::save)

    val event = EventEntity(
      user = testUserDetailsEntity,
      assessment = assessment,
      createdAt = assessment.createdAt,
      data = AssessmentCreatedEvent(
        formVersion = "1",
        properties = emptyMap(),
        timeline = null,
      ),
    ).run(eventRepository::save)

    val aggregateData = AssessmentAggregate().apply {
      collaborators.add(Collaborator.from(event.user))
      formVersion = event.data.formVersion
    }

    AggregateEntity(
      assessment = assessment,
      eventsFrom = event.createdAt,
      eventsTo = event.createdAt,
      data = aggregateData,
    )
      .apply { numberOfEventsApplied = 1 }
      .run(aggregateRepository::save)

    val request = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = UuidIdentifier(assessment.uuid),
          timestamp = LocalDateTime.parse("2025-01-01T08:00:01"),
        ),
      ),
    )

    val response = webTestClient.post().uri("/query")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.status).isEqualTo(400)
    assertThat(response?.userMessage).isEqualTo("Invalid timestamp 2025-01-01T08:00:01")
    assertThat(response?.developerMessage).isEqualTo("Timestamp cannot be before the assessment created date")
  }

  companion object {
    @JvmStatic
    fun assessmentAndIdentifierProvider() = AssessmentEntity(type = "TEST").apply {
      identifiers.add(
        AssessmentIdentifierEntity(
          identifierType = IdentifierType.CRN,
          identifier = UUID.randomUUID().toString(),
          assessment = this,
        ),
      )
    }.let { assessment ->
      listOf(
        Arguments.of(assessment, UuidIdentifier(assessment.uuid)),
        Arguments.of(
          assessment,
          with(assessment.identifiers.first()) {
            ExternalIdentifier(
              identifier = identifier,
              identifierType = identifierType,
              assessmentType = assessment.type,
            )
          },
        ),
      )
    }
  }
}
