package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.CollectionItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.CollectionItemAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class CollectionItemAnswersUpdatedEventHandlerTest : AbstractEventHandlerTest<CollectionItemAnswersUpdatedEvent, AssessmentState>() {
  override val handler = CollectionItemAnswersUpdatedEventHandler::class
  override val eventType = CollectionItemAnswersUpdatedEvent::class
  val aggregateUuid: UUID = UUID.randomUUID()

  override val scenarios = listOf(
    Scenario.Executes<CollectionItemAnswersUpdatedEvent, AssessmentState>("handles the event").apply {

      val collectionUuid: UUID = UUID.randomUUID()
      val collectionItemUuid: UUID = UUID.randomUUID()
      val collectionItemAnswersUpdatedEvent = eventEntityFor(
        CollectionItemAnswersUpdatedEvent(
          collectionItemUuid = collectionItemUuid,
          added = mapOf("foo" to listOf("foo_value")),
          removed = listOf("bar"),
          timeline = timeline,
        ),
      )

      events = listOf(collectionItemAnswersUpdatedEvent)

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
                  uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("bar" to listOf("bar_value")),
                        properties = mutableMapOf("baz" to listOf("baz_value")),
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
                  uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to listOf("foo_value")),
                        properties = mutableMapOf("baz" to listOf("baz_value")),
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
    Scenario.Executes<CollectionItemAnswersUpdatedEvent, AssessmentState>("handles when no timeline provided").apply {

      val collectionUuid: UUID = UUID.randomUUID()
      val collectionItemUuid: UUID = UUID.randomUUID()
      val collectionItemAnswersUpdatedEvent = eventEntityFor(
        CollectionItemAnswersUpdatedEvent(
          collectionItemUuid = collectionItemUuid,
          added = mapOf("foo" to listOf("foo_value")),
          removed = listOf("bar"),
          timeline = null,
        ),
      )

      events = listOf(collectionItemAnswersUpdatedEvent)

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
                  uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("bar" to listOf("bar_value")),
                        properties = mutableMapOf("baz" to listOf("baz_value")),
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
                  uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collection(
                    uuid = collectionUuid,
                    createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                    name = "TOP_LEVEL_COLLECTION",
                    items = mutableListOf(
                      CollectionItem(
                        uuid = collectionItemUuid,
                        createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        updatedAt = LocalDateTime.parse("2025-01-01T12:00:00"),
                        answers = mutableMapOf("foo" to listOf("foo_value")),
                        properties = mutableMapOf("baz" to listOf("baz_value")),
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
