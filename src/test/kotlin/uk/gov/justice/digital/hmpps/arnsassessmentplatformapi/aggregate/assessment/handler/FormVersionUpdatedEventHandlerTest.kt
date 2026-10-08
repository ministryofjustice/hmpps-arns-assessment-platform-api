package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class FormVersionUpdatedEventHandlerTest : AbstractEventHandlerTest<FormVersionUpdatedEvent>() {
  override val handler = FormVersionUpdatedEventHandler::class
  override val eventType = FormVersionUpdatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<FormVersionUpdatedEvent>("handles the event").apply {
      commandTimeline = Timeline(type = "CUSTOM_TIMELINE", data = mapOf("foo" to "bar"))

      event =
        eventEntityFor(
          FormVersionUpdatedEvent(
            version = "1",
          ),
        )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "version" to event.data.version,
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
            data = AssessmentAggregate().apply {
              formVersion = "1"
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
            eventsTo = event.createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = event.data.version
              collaborators.add(user.uuid)
            },
          ),
        )
      }
    },
    Scenario.Executes<FormVersionUpdatedEvent>("handles when no timeline provided").apply {
      event =
        eventEntityFor(
          FormVersionUpdatedEvent(
            version = "1",
          ),
        )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "version" to event.data.version,
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
            data = AssessmentAggregate().apply {
              formVersion = "1"
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
            eventsTo = event.createdAt,
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate().apply {
              formVersion = event.data.version
              collaborators.add(user.uuid)
            },
          ),
        )
      }
    },
  )
}
