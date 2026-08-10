package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentFlagsUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class AssessmentFlagsUpdatedEventHandlerTest : AbstractEventHandlerTest<AssessmentFlagsUpdatedEvent>() {
  override val handler = AssessmentFlagsUpdatedEventHandler::class
  override val eventType = AssessmentFlagsUpdatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<AssessmentFlagsUpdatedEvent>("handles the event by replacing flags").apply {
      commandTimeline = Timeline(type = "CUSTOM_TIMELINE", data = mapOf("foo" to "bar"))
      event = eventEntityFor(
        AssessmentFlagsUpdatedEvent(
          flags = listOf("SAN_BETA"),
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf("flags" to event.data.flags),
        commandTimeline,
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
              formVersion = "1"
              collaborators.add(user.uuid)
              flags.add("SAN_BETA")
            },
          ),
        )
      }
    },
    Scenario.Executes<AssessmentFlagsUpdatedEvent>("replaces existing flags with new flags").apply {
      event = eventEntityFor(
        AssessmentFlagsUpdatedEvent(
          flags = emptyList(),
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf("flags" to event.data.flags),
        null,
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate().apply {
              formVersion = "1"
              flags.add("SAN_BETA")
            },
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
              formVersion = "1"
              collaborators.add(user.uuid)
            },
          ),
        )
      }
    },
  )
}
