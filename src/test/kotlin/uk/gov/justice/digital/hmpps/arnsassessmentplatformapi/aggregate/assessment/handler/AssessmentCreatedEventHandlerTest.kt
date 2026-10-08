package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class AssessmentCreatedEventHandlerTest : AbstractEventHandlerTest<AssessmentCreatedEvent>() {
  override val handler = AssessmentCreatedEventHandler::class
  override val eventType = AssessmentCreatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<AssessmentCreatedEvent>("handles the event").apply {
      commandTimeline = Timeline(type = "CUSTOM_TIMELINE", data = mapOf("foo" to "bar"))
      event = eventEntityFor(
        AssessmentCreatedEvent(
          formVersion = "1",
          properties = mutableMapOf(),
        ),
      )
      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "formVersion" to event.data.formVersion,
          "properties" to event.data.properties.keys,
        ),
        commandTimeline,
      )
      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate(),
            assessment = assessment,
            updatedAt = now,
            eventsTo = now,
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
              formVersion = event.data.formVersion
              collaborators.add(user.uuid)
            },
          ),
        )
      }
    },
    Scenario.Executes<AssessmentCreatedEvent>("handles when no timeline provided").apply {
      event = eventEntityFor(
        AssessmentCreatedEvent(
          formVersion = "1",
          properties = mutableMapOf(),
        ),
      )
      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "formVersion" to event.data.formVersion,
          "properties" to event.data.properties.keys,
        ),
        null,
      )
      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate(),
            assessment = assessment,
            updatedAt = now,
            eventsTo = now,
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
              formVersion = event.data.formVersion
              collaborators.add(user.uuid)
            },
          ),
        )
      }
    },
  )
}
