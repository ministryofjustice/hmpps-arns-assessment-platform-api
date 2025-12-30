package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.PropertyNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentPropertiesUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class AssessmentPropertiesUpdatedEventHandlerTest : AbstractEventHandlerTest<AssessmentPropertiesUpdatedEvent>() {
  override val handler = AssessmentPropertiesUpdatedEventHandler::class
  override val eventType = AssessmentPropertiesUpdatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<AssessmentPropertiesUpdatedEvent>("handles the event").apply {
      events = listOf(
        eventEntityFor(
          AssessmentPropertiesUpdatedEvent(
            added = mapOf("foo" to SingleValue("foo_value")),
            removed = listOf("bar"),
            timeline = timeline,
          ),
        ),
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate().apply {
              formVersion = "1"
              properties.put("bar", SingleValue("value_to_remove"))
            },
            assessment = assessment,
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
              events.forEach { it.data.added.forEach { (key, value) -> properties.put(key, value) } }
              events.flatMap { it.data.removed }.forEach { deletedProperties.put(it, SingleValue("value_to_remove")) }
              timeline.add(TimelineItem("test", LocalDateTime.parse("2025-01-01T12:00:00"), mapOf("foo" to listOf("bar"))))
            },
          ),
        )
      }
    },
    Scenario.Executes<AssessmentPropertiesUpdatedEvent>("handles when no timeline provided").apply {
      events = listOf(
        eventEntityFor(
          AssessmentPropertiesUpdatedEvent(
            added = mapOf("foo" to SingleValue("foo_value")),
            removed = listOf("bar"),
            timeline = null,
          ),
        ),
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate().apply {
              formVersion = "1"
              properties.put("bar", SingleValue("value_to_remove"))
            },
            assessment = assessment,
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
              events.forEach { it.data.added.forEach { (key, value) -> properties.put(key, value) } }
              events.flatMap { it.data.removed }.forEach { deletedProperties.put(it, SingleValue("value_to_remove")) }
            },
          ),
        )
      }
    },
    Scenario.Throws<AssessmentPropertiesUpdatedEvent, PropertyNotFoundException>("throws when the property to remove does no exist").apply {
      events = listOf(
        eventEntityFor(
          AssessmentPropertiesUpdatedEvent(
            added = mapOf(),
            removed = listOf("bar"),
            timeline = null,
          ),
        ),
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate().apply {
              formVersion = "1"
            },
            assessment = assessment,
          ),
        )
      }

      expectedException = PropertyNotFoundException::class
    },
  )
}
