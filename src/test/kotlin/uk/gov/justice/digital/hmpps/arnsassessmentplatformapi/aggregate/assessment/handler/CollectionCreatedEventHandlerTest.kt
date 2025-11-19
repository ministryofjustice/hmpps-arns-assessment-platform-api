package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class CollectionCreatedEventHandlerTest : AbstractEventHandlerTest<CollectionCreatedEvent>() {
  override val handler = CollectionCreatedEventHandler::class
  override val eventType = CollectionCreatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<CollectionCreatedEvent>("handles the event").apply {
      val newTopLevelCollectionEvent = eventEntityFor(
        CollectionCreatedEvent(
          collectionUuid = UUID.randomUUID(),
          name = "NEW_TOP_LEVEL_COLLECTION",
          parentCollectionItemUuid = null,
          timeline = timeline,
        ),
      )
      val newChildCollectionEvent = eventEntityFor(
        CollectionCreatedEvent(
          collectionUuid = UUID.randomUUID(),
          name = "CHILD_LEVEL_COLLECTION",
          parentCollectionItemUuid = UUID.randomUUID(),
          timeline = timeline,
        ),
      )

      events = listOf(newTopLevelCollectionEvent, newChildCollectionEvent)

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
                    uuid = UUID.randomUUID(),
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = newChildCollectionEvent.data.parentCollectionItemUuid!!,
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
            eventsTo = events.last().createdAt,
            numberOfEventsApplied = 2,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = "1"
              collaborators.add(user)
              collections.addAll(
                listOf(
                  Collection(
                    uuid = initialState.aggregates.first().data.collections.first().uuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = newChildCollectionEvent.data.parentCollectionItemUuid!!,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf(),
                        properties = mutableMapOf(),
                        collections = mutableListOf(
                          Collection(
                            uuid = newChildCollectionEvent.data.collectionUuid,
                            createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                            updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                            name = "CHILD_LEVEL_COLLECTION",
                            items = mutableListOf(),
                          ),
                        ),
                      ),
                    ),
                  ),
                  Collection(
                    uuid = newTopLevelCollectionEvent.data.collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "NEW_TOP_LEVEL_COLLECTION",
                    items = mutableListOf(),
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
    Scenario.Executes<CollectionCreatedEvent>("handles when no timeline provided").apply {
      events = listOf(
        eventEntityFor(
          CollectionCreatedEvent(
            collectionUuid = UUID.randomUUID(),
            name = "NEW_TOP_LEVEL_COLLECTION",
            parentCollectionItemUuid = null,
            timeline = null,
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
              collaborators.add(user)
              collections.addAll(
                listOf(
                  Collection(
                    uuid = events.first().data.collectionUuid,
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
