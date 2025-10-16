// package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.query
//
// import org.assertj.core.api.Assertions
// import org.junit.jupiter.api.AfterEach
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentVersionAggregate
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdated
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdated
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
// import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
// import java.time.LocalDateTime
// import java.util.UUID
//
// class GetAggregateTest(
//  @Autowired
//  private val assessmentRepository: AssessmentRepository,
//  @Autowired
//  private val aggregateRepository: AggregateRepository,
//  @Autowired
//  private val eventRepository: EventRepository,
// ) : IntegrationTestBase() {
//  @BeforeEach
//  fun setUp() {
//  }
//
//  @AfterEach
//  fun tearDown() {
//  }
//
//  @Test
//  fun `it fetches the latest aggregate for an assessment`() {
//    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)
//
//    val events = listOf(
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
//            data = AssessmentCreated(),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
//            data = FormVersionUpdated("1"),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
//            data = AnswersUpdated(
//                added = mapOf("foo" to listOf("foo_value")),
//                removed = emptyList(),
//            ),
//        ),
//    ).run(eventRepository::saveAll)
//
//    val aggregateData = AssessmentVersionAggregate(
//        answers = mutableMapOf("foo" to listOf("foo_value")),
//        deletedAnswers = mutableMapOf(),
//        collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
//        formVersion = "1",
//    ).apply { numberOfEventsApplied = events.size.toLong() }
//
//    AggregateEntity(
//        assessment = assessment,
//        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
//        eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
//        data = aggregateData,
//    ).run(aggregateRepository::save)
//
//    val result = webTestClient.get().uri("/aggregate/${AssessmentVersionAggregate.Companion.aggregateType}/${assessment.uuid}")
//      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
//      .exchange()
//      .expectStatus().isOk
//      .expectBody(AssessmentVersionResponse::class.java)
//      .returnResult()
//      .responseBody
//
//    assertThat(result).isNotNull
//
//    assertThat(result?.answers).isEqualTo(aggregateData.getAnswers())
//    assertThat(result?.collaborators).isEqualTo(aggregateData.getCollaborators())
//    assertThat(result?.formVersion).isEqualTo(aggregateData.getFormVersion())
//  }
//
//  @Test
//  fun `it fetches an aggregate for a point in time`() {
//    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)
//
//    val events = listOf(
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
//            data = AssessmentCreated(),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
//            data = FormVersionUpdated("1"),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
//            data = AnswersUpdated(
//                added = mapOf("foo" to listOf("foo_value")),
//                removed = emptyList(),
//            ),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
//            data = AnswersUpdated(
//                added = mapOf("foo" to listOf("updated_foo_value")),
//                removed = emptyList(),
//            ),
//        ),
//    ).run(eventRepository::saveAll)
//
//    val firstAggregateData = AssessmentVersionAggregate(
//        answers = mutableMapOf("foo" to listOf("foo_value")),
//        deletedAnswers = mutableMapOf(),
//        collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
//        formVersion = "1",
//    ).apply { numberOfEventsApplied = 1 }
//
//    val secondAggregateData = AssessmentVersionAggregate(
//        answers = mutableMapOf("foo" to listOf("updated_foo_value")),
//        deletedAnswers = mutableMapOf(),
//        collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
//        formVersion = "1",
//    ).apply { numberOfEventsApplied = 2 }
//
//    listOf(
//        AggregateEntity(
//            assessment = assessment,
//            eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
//            eventsTo = LocalDateTime.parse("2025-01-01T12:30:00"),
//            data = secondAggregateData,
//        ),
//        AggregateEntity(
//            assessment = assessment,
//            eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
//            eventsTo = LocalDateTime.parse("2025-01-01T12:05:00"),
//            data = firstAggregateData,
//        ),
//    ).run(aggregateRepository::saveAll)
//
//    val result = webTestClient.get()
//      .uri("/aggregate/${AssessmentVersionAggregate.Companion.aggregateType}/${assessment.uuid}?timestamp=2025-01-01T12:15:00")
//      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
//      .exchange()
//      .expectStatus().isOk
//      .expectBody(AssessmentVersionResponse::class.java)
//      .returnResult()
//      .responseBody
//
//    assertThat(result).isNotNull
//
//    assertThat(result?.answers).isEqualTo(firstAggregateData.getAnswers())
//    assertThat(result?.collaborators).isEqualTo(firstAggregateData.getCollaborators())
//    assertThat(result?.formVersion).isEqualTo(firstAggregateData.getFormVersion())
//  }
//
//  @Test
//  fun `it creates an aggregate for an assessment where none exists`() {
//    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)
//
//    val events = listOf(
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
//            data = AssessmentCreated(),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
//            data = FormVersionUpdated("1"),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
//            data = AnswersUpdated(
//                added = mapOf("foo" to listOf("foo_value")),
//                removed = emptyList(),
//            ),
//        ),
//        EventEntity(
//            user = User("FOO_USER", "Foo User"),
//            assessment = assessment,
//            createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
//            data = AnswersUpdated(
//                added = mapOf("foo" to listOf("updated_foo_value")),
//                removed = emptyList(),
//            ),
//        ),
//    ).run(eventRepository::saveAll)
//
//    val aggregateData = AssessmentVersionAggregate(
//        answers = mutableMapOf("foo" to listOf("updated_foo_value")),
//        deletedAnswers = mutableMapOf(),
//        collaborators = mutableSetOf(User("FOO_USER", "Foo User")),
//        formVersion = "1",
//    ).apply { numberOfEventsApplied = events.size.toLong() }
//
//    val result = webTestClient.get().uri("/aggregate/${AssessmentVersionAggregate.Companion.aggregateType}/${assessment.uuid}")
//      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
//      .exchange()
//      .expectStatus().isOk
//      .expectBody(AssessmentVersionResponse::class.java)
//      .returnResult()
//      .responseBody
//
//    assertThat(result).isNotNull
//
//    assertThat(result?.answers).isEqualTo(aggregateData.getAnswers())
//    assertThat(result?.collaborators).isEqualTo(aggregateData.getCollaborators())
//    assertThat(result?.formVersion).isEqualTo(aggregateData.getFormVersion())
//
//    val persistedAggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
//      assessment.uuid,
//      AssessmentVersionAggregate.Companion.aggregateType,
//      LocalDateTime.now(),
//    )
//
//    Assertions.assertThat(persistedAggregate?.data).isNotNull
//    Assertions.assertThat(persistedAggregate?.data?.numberOfEventsApplied).isEqualTo(3)
//  }
//
//  @Test
//  fun `it returns 404 when there is no assessments`() {
//    webTestClient.get().uri("/aggregate/${AssessmentVersionAggregate.Companion.aggregateType}/${UUID.randomUUID()}")
//      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
//      .exchange()
//      .expectStatus().isNotFound
//  }
//
//  @Test
//  fun `it returns 400 when an aggregate does not exist for a given type`() {
//    val assessment: AssessmentEntity = AssessmentEntity().run(assessmentRepository::save)
//
//    webTestClient.get().uri("/aggregate/UNKNOWN_AGGREGATE_TYPE/${assessment.uuid}")
//      .headers(setAuthorisation(roles = listOf("ROLE_ARNS_ASSESSMENT_PLATFORM_READ")))
//      .exchange()
//      .expectStatus().isBadRequest
//  }
// }
