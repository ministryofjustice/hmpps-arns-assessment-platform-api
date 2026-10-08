package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class CollectionCreatedEventHandlerTest : AbstractEventHandlerTest<CollectionCreatedEvent>() {
  override val handler = CollectionCreatedEventHandler::class
  override val eventType = CollectionCreatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<CollectionCreatedEvent>("handles the event").apply {
      val existingCollectionItemUuid = UUID.randomUUID()

      commandTimeline = Timeline(type = "CUSTOM_TIMELINE", data = mapOf("foo" to "bar"))

      event = eventEntityFor(
        CollectionCreatedEvent(
          collectionUuid = UUID.randomUUID(),
          name = "CHILD_LEVEL_COLLECTION",
          parentCollectionItemUuid = existingCollectionItemUuid,
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "collection" to event.data.name,
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
                    uuid = UUID.randomUUID(),
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = existingCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf(),
                        properties = mutableMapOf(),
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
                    uuid = initialState.aggregates.first().data.collections.first().uuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = existingCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf(),
                        properties = mutableMapOf(),
                        collections = mutableListOf(
                          Collection(
                            uuid = event.data.collectionUuid,
                            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                            updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                            name = "CHILD_LEVEL_COLLECTION",
                            items = mutableListOf(),
                          ),
                        ),
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
    Scenario.Executes<CollectionCreatedEvent>("handles when no timeline provided").apply {
      event = eventEntityFor(
        CollectionCreatedEvent(
          collectionUuid = UUID.randomUUID(),
          name = "NEW_TOP_LEVEL_COLLECTION",
          parentCollectionItemUuid = null,
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "collection" to event.data.name,
          "collectionUuid" to event.data.collectionUuid,
        ),
        null,
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
                    uuid = event.data.collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "NEW_TOP_LEVEL_COLLECTION",
                    items = mutableListOf(),
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
