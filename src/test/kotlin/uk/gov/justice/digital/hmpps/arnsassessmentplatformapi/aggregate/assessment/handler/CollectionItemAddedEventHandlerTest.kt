package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collaborator
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class CollectionItemAddedEventHandlerTest : AbstractEventHandlerTest<CollectionItemAddedEvent>() {
  override val handler = CollectionItemAddedEventHandler::class
  override val eventType = CollectionItemAddedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<CollectionItemAddedEvent>("handles the event").apply {
      val collectionUuid: UUID = UUID.randomUUID()
      val collectionItemUuid: UUID = UUID.randomUUID()

      events = listOf(
        eventEntityFor(
          CollectionItemAddedEvent(
            collectionUuid = collectionUuid,
            collectionItemUuid = collectionItemUuid,
            answers = mapOf("foo" to SingleValue("first_foo")),
            properties = mapOf("bar" to SingleValue("first_bar")),
            index = 0,
            timeline = timeline,
          ),
        ),
        eventEntityFor(
          CollectionItemAddedEvent(
            collectionUuid = collectionUuid,
            collectionItemUuid = collectionItemUuid,
            answers = mapOf("foo" to SingleValue("second_foo")),
            properties = mapOf("bar" to SingleValue("second_bar")),
            index = null,
            timeline = timeline,
          ),
        ),
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collections.addAll(
                listOf(
                  Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("existing_foo")),
                        properties = mutableMapOf("bar" to SingleValue("existing_bar")),
                        collections = mutableListOf(),
                      ),
                    ),
                  ),
                ),
              )
            },
          ),
        )
      }

      expectedState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            eventsTo = events.last().createdAt,
            numberOfEventsApplied = 2,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(Collaborator.from(user))
              collections.addAll(
                listOf(
                  Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("first_foo")),
                        properties = mutableMapOf("bar" to SingleValue("first_bar")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("existing_foo")),
                        properties = mutableMapOf("bar" to SingleValue("existing_bar")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("second_foo")),
                        properties = mutableMapOf("bar" to SingleValue("second_bar")),
                        collections = mutableListOf(),
                      ),
                    ),
                  ),
                ),
              )
              timeline.addAll(
                listOf(
                  TimelineItem(
                    "test",
                    LocalDateTime.parse("2025-01-01T12:00:00"),
                    mapOf("foo" to listOf("bar")),
                  ),
                  TimelineItem(
                    "test",
                    LocalDateTime.parse("2025-01-01T12:00:00"),
                    mapOf("foo" to listOf("bar")),
                  ),
                ),
              )
            },
          ),
        )
      }
    },
    Scenario.Executes<CollectionItemAddedEvent>("handles when no timeline provided").apply {
      val collectionUuid: UUID = UUID.randomUUID()
      val collectionItemUuid: UUID = UUID.randomUUID()
      val collectionItemAddedEvent = eventEntityFor(
        CollectionItemAddedEvent(
          collectionUuid = collectionUuid,
          collectionItemUuid = collectionItemUuid,
          answers = mapOf("foo" to SingleValue("foo_value")),
          properties = mapOf("bar" to SingleValue("bar_value")),
          index = 0,
          timeline = null,
        ),
      )

      events = listOf(collectionItemAddedEvent)

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collections.addAll(
                listOf(
                  Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(),
                  ),
                ),
              )
            },
          ),
        )
      }

      expectedState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            eventsTo = events.last().createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(Collaborator.from(user))
              collections.addAll(
                listOf(
                  Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("foo_value")),
                        properties = mutableMapOf("bar" to SingleValue("bar_value")),
                        collections = mutableListOf(),
                      ),
                    ),
                  ),
                ),
              )
            },
          ),
        )
      }
    },
  )
}
