package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class AssessmentCreatedEventHandlerTest : AbstractEventHandlerTest<AssessmentCreatedEvent>() {
  override val handler = AssessmentCreatedEventHandler::class
  override val eventType = AssessmentCreatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<AssessmentCreatedEvent>("handles the event").apply {
      events = listOf(
        eventEntityFor(
          AssessmentCreatedEvent(
            formVersion = "1",
            properties = mutableMapOf(),
            timeline = timeline,
          ),
        ),
      )
      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate(),
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
              formVersion = events.last().data.formVersion
              collaborators.add(user)
              timeline.add(
                TimelineItem(
                  "test",
                  LocalDateTime.parse("2025-01-01T12:00:00"),
                  mapOf("foo" to listOf("bar")),
                ),
              )
            },
          ),
        )
      }
    },
    Scenario.Executes<AssessmentCreatedEvent>("handles when no timeline provided").apply {
      events = listOf(
        eventEntityFor(
          AssessmentCreatedEvent(
            formVersion = "1",
            properties = mutableMapOf(),
            timeline = null,
          ),
        ),
      )
      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate(),
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
              formVersion = events.last().data.formVersion
              collaborators.add(user)
            },
          ),
        )
      }
    },
  )
}
