package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.Collaborator
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.GroupEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime

class GroupEventHandlerTest : AbstractEventHandlerTest<GroupEvent>() {
  override val handler = GroupEventHandler::class
  override val eventType = GroupEvent::class

  override val scenarios = listOf(
    Scenario.Executes<GroupEvent>("handles the event").apply {
      events = listOf(
        eventEntityFor(
          GroupEvent(
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
              collaborators.add(Collaborator.from(user))
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
    Scenario.Executes<GroupEvent>("handles when no timeline provided").apply {
      events = listOf(
        eventEntityFor(
          GroupEvent(
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
              collaborators.add(Collaborator.from(user))
            },
          ),
        )
      }
    },
  )
}
