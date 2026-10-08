package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.exception.AnswerNotFoundException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.Timeline
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class AssessmentAnswersUpdatedEventHandlerTest : AbstractEventHandlerTest<AssessmentAnswersUpdatedEvent>() {
  override val handler = AssessmentAnswersUpdatedEventHandler::class
  override val eventType = AssessmentAnswersUpdatedEvent::class

  override val scenarios = listOf(
    Scenario.Executes<AssessmentAnswersUpdatedEvent>("handles the event").apply {
      commandTimeline = Timeline(type = "CUSTOM_TIMELINE", data = mapOf("foo" to "bar"))

      event = eventEntityFor(
        AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = listOf("bar"),
        ),
      )

      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "added" to event.data.added.keys,
          "removed" to event.data.removed,
        ),
        commandTimeline,
      )

      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate().apply {
              formVersion = "1"
              answers["bar"] = SingleValue("value_to_remove")
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
              answers["foo"] = SingleValue("foo_value")
            },
          ),
        )
      }
    },
    Scenario.Executes<AssessmentAnswersUpdatedEvent>("handles when no timeline provided").apply {
      event = eventEntityFor(
        AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to SingleValue("foo_value")),
          removed = listOf("bar"),
        ),
      )
      expectedTimeline = timelineEntityFor(
        event,
        mapOf(
          "added" to event.data.added.keys,
          "removed" to event.data.removed,
        ),
        null,
      )
      initialState = AssessmentState().also { state ->
        state.aggregates.add(
          AggregateEntity(
            uuid = aggregateUuid,
            eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
            data = AssessmentAggregate().apply {
              formVersion = "1"
              answers["bar"] = SingleValue("value_to_remove")
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
              answers["foo"] = SingleValue("foo_value")
            },
          ),
        )
      }
    },
    Scenario.Throws<AssessmentAnswersUpdatedEvent, AnswerNotFoundException>("throws when answer to remove does not exist").apply {
      event = eventEntityFor(
        AssessmentAnswersUpdatedEvent(
          added = mapOf(),
          removed = listOf("foo"),
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
            updatedAt = now,
            eventsTo = now,
          ),
        )
      }

      expectedException = AnswerNotFoundException::class
    },
  )
}
