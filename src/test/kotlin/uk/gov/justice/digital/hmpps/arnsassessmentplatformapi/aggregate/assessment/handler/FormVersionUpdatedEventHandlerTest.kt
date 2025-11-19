package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class FormVersionUpdatedEventHandlerTest : AbstractEventHandlerTest<FormVersionUpdatedEvent, AssessmentState>() {
  override val handler = FormVersionUpdatedEventHandler::class
  override val eventType = FormVersionUpdatedEvent::class
  val aggregateUuid: UUID = UUID.randomUUID()

  override val scenarios = listOf(
    Scenario.Executes<FormVersionUpdatedEvent, AssessmentState>("handles the event").apply {
      events = listOf(
        eventEntityFor(
          FormVersionUpdatedEvent(
            version = "1",
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
              formVersion = events.last().data.version
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
    Scenario.Executes<FormVersionUpdatedEvent, AssessmentState>("handles when no timeline provided").apply {
      events = listOf(
        eventEntityFor(
          FormVersionUpdatedEvent(
            version = "1",
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
              formVersion = events.last().data.version
              collaborators.add(user)
            },
          ),
        )
      }
    },
  )
}
