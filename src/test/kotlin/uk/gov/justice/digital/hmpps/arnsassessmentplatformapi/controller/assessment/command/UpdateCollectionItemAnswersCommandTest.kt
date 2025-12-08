package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateCollectionItemAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.config.Clock
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class UpdateCollectionItemAnswersCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val aggregateRepository: AggregateRepository,
) : IntegrationTestBase() {
  @Autowired
  private lateinit var eventRepository: EventRepository

  @BeforeEach
  fun setUp() {
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun `it updates the answers in a collection item`() {
    val assessmentEntity = AssessmentEntity(createdAt = LocalDateTime.parse("2025-01-01T12:35:00"))
    assessmentRepository.save(assessmentEntity)

    val collectionUuid = UUID.randomUUID()
    val collectionItemToBeUnchangedUuid = UUID.randomUUID()
    val collectionItemToBeUpdatedUuid = UUID.randomUUID()
    val aggregateEntity = AggregateEntity(
      assessment = assessmentEntity,
      updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
      eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
      data = AssessmentAggregate().apply {
        formVersion = "1"
        collections.add(
          Collection(
            uuid = collectionUuid,
            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
            updatedAt = LocalDateTime.parse("2025-01-01T13:00:00"),
            name = "COLLECTION_NAME",
            items = mutableListOf(
              CollectionItem(
                uuid = collectionItemToBeUnchangedUuid,
                createdAt = LocalDateTime.parse("2025-01-01T12:10:00"),
                updatedAt = LocalDateTime.parse("2025-01-01T12:10:00"),
                answers = mutableMapOf("title" to SingleValue("unchanged_1")),
                properties = mutableMapOf(),
                collections = mutableListOf(),
              ),
              CollectionItem(
                uuid = collectionItemToBeUpdatedUuid,
                createdAt = LocalDateTime.parse("2025-01-01T12:20:00"),
                updatedAt = LocalDateTime.parse("2025-01-01T12:20:00"),
                answers = mutableMapOf(
                  "title" to SingleValue("unchanged_2"),
                  "to_be_removed" to SingleValue("should_not_exist"),
                ),
                properties = mutableMapOf(),
                collections = mutableListOf(),
              ),
            ),
          ),
        )
      },
    )
    aggregateRepository.save(aggregateEntity)

    val user = User("FOO_USER", "Foo User")

    eventRepository.saveAll(
      listOf(
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          data = AssessmentCreatedEvent(
            formVersion = "1",
            properties = emptyMap(),
            timeline = null,
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:05:00"),
          data = CollectionCreatedEvent(
            collectionUuid = collectionUuid,
            name = "COLLECTION_NAME",
            parentCollectionItemUuid = null,
            timeline = null,
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:10:00"),
          data = CollectionItemAddedEvent(
            collectionUuid = collectionUuid,
            collectionItemUuid = UUID.randomUUID(),
            answers = mutableMapOf("title" to SingleValue("unchanged_1")),
            properties = mutableMapOf(),
            index = null,
            timeline = null,
          ),
        ),
        EventEntity(
          user = user,
          assessment = assessmentEntity,
          createdAt = LocalDateTime.parse("2025-01-01T12:20:00"),
          data = CollectionItemAddedEvent(
            collectionUuid = collectionUuid,
            collectionItemUuid = UUID.randomUUID(),
            answers = mutableMapOf(
              "title" to SingleValue("unchanged_2"),
              "to_be_removed" to SingleValue("should_not_exist"),
            ),
            properties = mutableMapOf(),
            index = null,
            timeline = null,
          ),
        ),
      ),
    )

    val request = CommandsRequest(
      commands = listOf(
        UpdateCollectionItemAnswersCommand(
          user = User("test-user", "Test User"),
          assessmentUuid = assessmentEntity.uuid,
          collectionItemUuid = collectionItemToBeUpdatedUuid,
          added = mutableMapOf("title" to SingleValue("updated")),
          removed = listOf("to_be_removed"),
          timeline = null,
        ),
      ),
    )

    val response = webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.commands).hasSize(1)
    assertThat(response?.commands?.first()?.request).isEqualTo(request.commands.first())
    assertIs<CommandSuccessCommandResult>(response?.commands?.first()?.result)

    val eventsForAssessment = eventRepository.findAllByAssessmentUuid(assessmentEntity.uuid)

    assertThat(eventsForAssessment.size).isEqualTo(5)
    assertThat(eventsForAssessment.last().data).isInstanceOf(CollectionItemAnswersUpdatedEvent::class.java)

    val aggregate = aggregateRepository.findByAssessmentAndTypeBeforeDate(
      assessmentEntity.uuid,
      AssessmentAggregate::class.simpleName!!,
      Clock.now(),
    )

    assertNotNull(aggregate)
    val data = assertIs<AssessmentAggregate>(aggregate.data)
    val collection = data.collections.find { it.uuid == collectionUuid }
    assertNotNull(collection)
    assertThat(collection.items.size).isEqualTo(2)

    val updatedItem = collection.items.find { it.uuid == collectionItemToBeUpdatedUuid }
    assertNotNull(updatedItem)
    assertThat(updatedItem.answers["title"]).isEqualTo(SingleValue("updated"))
    assertNull(updatedItem.answers["to_be_removed"])

    val unchangedItem = collection.items.find { it.uuid == collectionItemToBeUnchangedUuid }
    assertNotNull(unchangedItem)
    assertThat(unchangedItem.answers["title"]).isEqualTo(SingleValue("unchanged_1"))
  }
}
