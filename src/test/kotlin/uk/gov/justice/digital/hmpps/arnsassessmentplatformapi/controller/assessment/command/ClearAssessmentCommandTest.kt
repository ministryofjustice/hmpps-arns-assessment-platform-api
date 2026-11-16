package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.assessment.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.ClearAssessmentCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.result.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentClearedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AggregateRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.EventRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.repository.TimelineRepository
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.UuidIdentifier
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentVersionQueryResult
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertIs

class ClearAssessmentCommandTest(
  @Autowired
  val assessmentRepository: AssessmentRepository,
  @Autowired
  val aggregateRepository: AggregateRepository,
  @Autowired
  val eventRepository: EventRepository,
) : IntegrationTestBase() {
  @Autowired
  private lateinit var timelineRepository: TimelineRepository

  @Test
  fun `it clears the assessment`() {
    val assessmentEntity = givenAnAssessmentWithData()

    val command = ClearAssessmentCommand(
      user = testUserDetails,
      assessmentUuid = assessmentEntity.uuid.toReference(),
      properties = listOf("*_section_complete", "is_user_submitted_*"),
      collections = listOf("OFFENCE_ANALYSIS_VICTIM"),
    )

    val response = webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(CommandsRequest(commands = listOf(command)))
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    assertIs<CommandSuccessCommandResult>(response?.commands[0]?.result)

    val events = eventRepository.findAllByAssessmentUuidOrderById(assessmentEntity.uuid)
    val cleared = assertIs<AssessmentClearedEvent>(events.last().data)
    assertThat(cleared.properties).containsExactlyInAnyOrder(
      "accommodation_section_complete",
      "is_user_submitted_accommodation",
    )
    assertThat(cleared.collections).containsExactly("OFFENCE_ANALYSIS_VICTIM")

    val aggregate = aggregateRepository.findTopByAssessmentUuidAndDataTypeAndEventsToLessThanEqualOrderByPositionDesc(
      assessmentEntity.uuid,
      AssessmentAggregate::class.simpleName!!,
      clock.now(),
    )
    val data = assertIs<AssessmentAggregate>(aggregate?.data)
    assertThat(data.answers).isEmpty()
    assertThat(data.properties.keys).containsExactly("keep_me")
    assertThat(data.collections.single { it.name == "OFFENCE_ANALYSIS_VICTIM" }.items).isEmpty()
  }

  @Test
  fun `it writes a timeline item`() {
    val assessmentEntity = givenAnAssessmentWithData()

    val command = ClearAssessmentCommand(
      user = testUserDetails,
      assessmentUuid = assessmentEntity.uuid.toReference(),
      properties = listOf("*_section_complete"),
      collections = listOf("OFFENCE_ANALYSIS_VICTIM"),
      timeline = Timeline(
        type = "PATHWAY_CHANGE",
        data = mapOf("from" to "COMMUNITY", "to" to "CUSTODY"),
      ),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(CommandsRequest(commands = listOf(command)))
      .exchange()
      .expectStatus().isOk

    val timeline = timelineRepository.findByAssessmentUuid(assessmentEntity.uuid)

    assertThat(timeline).hasSize(1)
    val item = timeline.single()
    assertThat(item.eventType).isEqualTo("AssessmentClearedEvent")
    assertThat(item.customType).isEqualTo("PATHWAY_CHANGE")
    assertThat(item.customData).containsExactlyInAnyOrderEntriesOf(mapOf("from" to "COMMUNITY", "to" to "CUSTODY"))
    assertThat(item.data["properties"]).isEqualTo(listOf("accommodation_section_complete"))
    assertThat(item.data["collections"]).isEqualTo(listOf("OFFENCE_ANALYSIS_VICTIM"))
  }

  @Test
  fun `it keeps the previous version in history`() {
    val assessmentEntity = givenAnAssessmentWithData()

    val command = ClearAssessmentCommand(
      user = testUserDetails,
      assessmentUuid = assessmentEntity.uuid.toReference(),
      properties = listOf("*_section_complete"),
      collections = listOf("OFFENCE_ANALYSIS_VICTIM"),
    )

    webTestClient.post().uri("/command")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(CommandsRequest(commands = listOf(command)))
      .exchange()
      .expectStatus().isOk

    // Latest version: cleared
    val latest = queryVersion(assessmentEntity)
    assertThat(latest.answers).isEmpty()
    assertThat(latest.properties).doesNotContainKey("accommodation_section_complete")

    // Before the clear: everything still there
    val before = queryVersion(assessmentEntity, LocalDateTime.parse("2025-01-01T12:30:00"))
    assertThat(before.answers).containsKey("some_answer")
    assertThat(before.properties).containsKey("accommodation_section_complete")
    assertThat(before.collections.single { it.name == "OFFENCE_ANALYSIS_VICTIM" }.items).hasSize(1)
  }

  @Test
  fun `it accepts the command as raw JSON`() {
    val assessmentEntity = givenAnAssessmentWithData()

    val request =
      """                                                                                                                                                                                  
        {                                                                                                                                                                                                
          "commands": [                                                                                                                                                                                  
            {                                                                                                                                                                                            
              "type": "ClearAssessmentCommand",                                                                                                                                                          
              "user": {                                                                                                                                                                                  
                "id": "${testUserDetails.id}",                                                                                                                                                           
                "name": "${testUserDetails.name}",                                                                                                                                                       
                "authSource": "${testUserDetails.authSource}"                                                                                                                                            
              },                                                                                                                                                                                         
              "assessmentUuid": "${assessmentEntity.uuid}",                                                                                                                                              
              "properties": ["*_section_complete", "is_user_submitted_*"],                                                                                                                               
              "collections": ["OFFENCE_ANALYSIS_VICTIM"],                                                                                                                                                
              "timeline": {                                                                                                                                                                              
                "type": "PATHWAY_CHANGE",                                                                                                                                                                
                "data": {}                                                                                                                                                                               
              }                                                                                                                                                                                          
            }                                                                                                                                                                                            
          ]                                                                                                                                                                                              
        }                                                                                                                                                                                                
      """.trimIndent()

    val response = webTestClient.post().uri("/command")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(CommandsResponse::class.java)
      .returnResult()
      .responseBody

    val command = assertIs<ClearAssessmentCommand>(response?.commands?.single()?.request)
    assertThat(command.properties).containsExactly("*_section_complete", "is_user_submitted_*")
    assertThat(command.collections).containsExactly("OFFENCE_ANALYSIS_VICTIM")
    assertThat(command.timeline?.type).isEqualTo("PATHWAY_CHANGE")
    assertIs<CommandSuccessCommandResult>(response?.commands?.single()?.result)

    val events = eventRepository.findAllByAssessmentUuidOrderById(assessmentEntity.uuid)
    assertIs<AssessmentClearedEvent>(events.last().data)
  }

  private fun givenAnAssessmentWithData(): AssessmentEntity {
    val collectionUuid = UUID.randomUUID()
    val itemUuid = UUID.randomUUID()
    val assessmentEntity = AssessmentEntity(type = "TEST", createdAt = LocalDateTime.parse("2025-01-01T12:00:00"))
    assessmentRepository.save(assessmentEntity)

    aggregateRepository.save(
      AggregateEntity(
        assessment = assessmentEntity,
        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsFrom = LocalDateTime.parse("2025-01-01T12:00:00"),
        eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
        position = 0,
        data = AssessmentAggregate().apply {
          formVersion = "1"
          answers["some_answer"] = SingleValue("yes")
          properties["accommodation_section_complete"] = SingleValue("YES")
          properties["is_user_submitted_accommodation"] = SingleValue("YES")
          properties["keep_me"] = SingleValue("still here")
          collections.add(
            Collection(
              uuid = collectionUuid,
              createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
              updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
              name = "OFFENCE_ANALYSIS_VICTIM",
              items = mutableListOf(
                CollectionItem(
                  uuid = itemUuid,
                  createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                  updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                  answers = mutableMapOf(),
                  properties = mutableMapOf(),
                  collections = mutableListOf(),
                ),
              ),
            ),
          )
        },
      ),
    )

    val at = LocalDateTime.parse("2025-01-01T12:00:00")

    eventRepository.saveAll(
      listOf(
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = at,
          position = 0,
          data = AssessmentCreatedEvent(
            formVersion = "1",
            properties = mapOf(
              "accommodation_section_complete" to SingleValue("YES"),
              "is_user_submitted_accommodation" to SingleValue("YES"),
              "keep_me" to SingleValue("still here"),
            ),
          ),
        ),
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = at,
          position = 1,
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf("some_answer" to SingleValue("yes")),
            removed = emptyList(),
          ),
        ),
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = at,
          position = 2,
          data = CollectionCreatedEvent(
            collectionUuid = collectionUuid,
            name = "OFFENCE_ANALYSIS_VICTIM",
            parentCollectionItemUuid = null,
          ),
        ),
        EventEntity(
          user = testUserDetailsEntity,
          assessment = assessmentEntity,
          createdAt = at,
          position = 3,
          data = CollectionItemAddedEvent(
            collectionItemUuid = itemUuid,
            collectionUuid = collectionUuid,
            answers = emptyMap(),
            properties = emptyMap(),
            index = null,
          ),
        ),
      ),
    )

    return assessmentEntity
  }

  private fun queryVersion(
    assessmentEntity: AssessmentEntity,
    timestamp: LocalDateTime? = null,
  ): AssessmentVersionQueryResult {
    val response = webTestClient.post().uri("/query")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_AAP__FRONTEND_RW")))
      .bodyValue(
        QueriesRequest(
          queries = listOf(
            AssessmentVersionQuery(
              user = testUserDetails,
              assessmentIdentifier = UuidIdentifier(assessmentEntity.uuid),
              timestamp = timestamp,
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(QueriesResponse::class.java)
      .returnResult()
      .responseBody

    return assertIs<AssessmentVersionQueryResult>(response?.queries?.single()?.result)
  }
}
