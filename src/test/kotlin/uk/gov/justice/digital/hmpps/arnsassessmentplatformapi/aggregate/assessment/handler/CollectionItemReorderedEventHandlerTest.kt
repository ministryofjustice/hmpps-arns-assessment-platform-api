package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.exception.CollectionItemNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemReorderedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class CollectionItemReorderedEventHandlerTest : AbstractEventHandlerTest<CollectionItemReorderedEvent>() {
  override val handler = CollectionItemReorderedEventHandler::class
  override val eventType = CollectionItemReorderedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<CollectionItemReorderedEvent>("handles the event").apply {
      val collectionUuid: UUID = UUID.randomUUID()
      val firstCollectionItemUuid: UUID = UUID.randomUUID()
      val secondCollectionItemUuid: UUID = UUID.randomUUID()
      val collectionItemRemovedEvent = eventEntityFor(
        CollectionItemReorderedEvent(
          collectionItemUuid = firstCollectionItemUuid,
          index = 0,
          timeline = timeline,
        ),
      )

      events = listOf(collectionItemRemovedEvent)

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
                        uuid = secondCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        answers = mutableMapOf("foo" to SingleValue("foo_value")),
                        properties = mutableMapOf("bar" to SingleValue("bar_value")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = firstCollectionItemUuid,
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
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = firstCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("foo_value")),
                        properties = mutableMapOf("bar" to SingleValue("bar_value")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = secondCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        answers = mutableMapOf("foo" to SingleValue("foo_value")),
                        properties = mutableMapOf("bar" to SingleValue("bar_value")),
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
                ),
              )
            },
          ),
        )
      }
    },
    Scenario.Executes<CollectionItemReorderedEvent>("handles when no timeline provided").apply {
      val collectionUuid: UUID = UUID.randomUUID()
      val firstCollectionItemUuid: UUID = UUID.randomUUID()
      val secondCollectionItemUuid: UUID = UUID.randomUUID()
      val collectionItemRemovedEvent = eventEntityFor(
        CollectionItemReorderedEvent(
          collectionItemUuid = firstCollectionItemUuid,
          index = 0,
          timeline = null,
        ),
      )

      events = listOf(collectionItemRemovedEvent)

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
                        uuid = secondCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        answers = mutableMapOf("foo" to SingleValue("foo_value")),
                        properties = mutableMapOf("bar" to SingleValue("bar_value")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = firstCollectionItemUuid,
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
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = firstCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to SingleValue("foo_value")),
                        properties = mutableMapOf("bar" to SingleValue("bar_value")),
                        collections = mutableListOf(),
                      ),
                      CollectionItem(
                        uuid = secondCollectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:30:00"),
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
    Scenario.Throws<CollectionItemReorderedEvent, CollectionItemNotFoundException>("throws when collection does not exist")
      .apply {
        events = listOf(
          eventEntityFor(
            CollectionItemReorderedEvent(
              collectionItemUuid = UUID.randomUUID(),
              index = 0,
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

        expectedException = CollectionItemNotFoundException::class
      },
  )
}
