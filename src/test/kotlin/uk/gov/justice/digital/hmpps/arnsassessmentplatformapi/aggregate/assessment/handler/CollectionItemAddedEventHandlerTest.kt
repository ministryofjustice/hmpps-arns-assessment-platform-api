package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAddedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class CollectionItemAddedEventHandlerTest : AbstractEventHandlerTest<CollectionItemAddedEvent>() {
  override val handler = CollectionItemAddedEventHandler::class
  override val eventType = CollectionItemAddedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<CollectionItemAddedEvent>("handles the event").apply {
      val existingCollectionUuid: UUID = UUID.randomUUID()
      val existingCollectionItemUuid: UUID = UUID.randomUUID()

      commandTimeline = Timeline(type = "CUSTOM_TIMELINE", data = mapOf("foo" to "bar"))

      event =
        eventEntityFor(
          CollectionItemAddedEvent(
            collectionUuid = existingCollectionUuid,
            collectionItemUuid = UUID.randomUUID(),
            answers = mapOf("foo" to SingleValue("first_foo")),
            properties = mapOf("bar" to SingleValue("first_bar")),
            index = 0,
          ),
        )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "index" to 0,
          "collection" to "TOP_LEVEL_COLLECTION",
          "collectionItemUuid" to event.data.collectionItemUuid,
          "collectionUuid" to event.data.collectionUuid,
        ),
        commandTimeline,
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            updatedAt = now,
            eventsTo = now,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collections.addAll(
                listOf(
                  Collection(
                    uuid = existingCollectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = existingCollectionItemUuid,
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
            eventsTo = event.createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(user.uuid)
              collections.addAll(
                listOf(
                  Collection(
                    uuid = existingCollectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = event.data.collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("first_foo")),
                        properties = mutableMapOf("bar" to SingleValue("first_bar")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = existingCollectionItemUuid,
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
    },
    Scenario.Executes<CollectionItemAddedEvent>("handles when no timeline provided").apply {
      val existingCollectionUuid: UUID = UUID.randomUUID()

      event =
        eventEntityFor(
          CollectionItemAddedEvent(
            collectionUuid = existingCollectionUuid,
            collectionItemUuid = UUID.randomUUID(),
            answers = mapOf("foo" to SingleValue("first_foo")),
            properties = mapOf("bar" to SingleValue("first_bar")),
            index = 0,
          ),
        )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "index" to 0,
          "collection" to "TOP_LEVEL_COLLECTION",
          "collectionItemUuid" to event.data.collectionItemUuid,
          "collectionUuid" to event.data.collectionUuid,
        ),
        commandTimeline,
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            updatedAt = now,
            eventsTo = now,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collections.addAll(
                listOf(
                  Collection(
                    uuid = existingCollectionUuid,
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
            eventsTo = event.createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(user.uuid)
              collections.addAll(
                listOf(
                  Collection(
                    uuid = existingCollectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = event.data.collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("first_foo")),
                        properties = mutableMapOf("bar" to SingleValue("first_bar")),
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
