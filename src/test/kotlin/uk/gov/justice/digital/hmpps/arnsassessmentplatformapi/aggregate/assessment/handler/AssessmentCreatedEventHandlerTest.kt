package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.handler

import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.AssessmentState
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import java.time.LocalDateTime
import java.util.UUID

class AssessmentCreatedEventHandlerTest : AbstractEventHandlerTest<AssessmentCreatedEvent, AssessmentState>() {
  override val handler = AssessmentCreatedEventHandler::class

  val aggregateUuid: UUID = UUID.randomUUID()

  override val events = listOf(
    eventEntityFor(
      AssessmentCreatedEvent(
        formVersion = "1",
        properties = mutableMapOf(),
        timeline = timeline,
      ),
    ),
  )

  override val initialState = AssessmentState().also { state ->
    state.aggregates.add(
      AggregateEntity(
        uuid = aggregateUuid,
        eventsFrom = LocalDateTime.parse("2025-01-01T09:00:00"),
        data = AssessmentAggregate(),
        assessment = assessment,
      ),
    )
  }

  override val expectedState = AssessmentState().also { state ->
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
          timeline.add(TimelineItem("test", LocalDateTime.parse("2025-01-01T12:00:00"), mapOf("foo" to listOf("bar"))))
        },
      ),
    )
  }
}
