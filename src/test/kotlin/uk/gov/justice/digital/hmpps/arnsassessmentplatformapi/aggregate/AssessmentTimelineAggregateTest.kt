package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersRolledBackEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentAnswersUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdatedEvent
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AssessmentTimelineAggregateTest {
  val assessment = AssessmentEntity()
  val user = User("FOO_USER", "Foo User")

  @Nested
  inner class Apply {
    @Test
    fun `it handles the AnswersUpdated event`() {
      val aggregate = AssessmentTimelineAggregate()
      aggregate.apply(
        EventEntity(
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          assessment = assessment,
          user = user,
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf(
              "foo" to listOf("foo_value"),
              "bar" to listOf("bar_value"),
            ),
            removed = emptyList(),
          ),
        ),
      )
      aggregate.apply(
        EventEntity(
          createdAt = LocalDateTime.parse("2025-01-01T12:30:00"),
          assessment = assessment,
          user = user,
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf(
              "foo" to listOf("updated_foo_value"),
              "baz" to listOf("baz_value"),
            ),
            removed = listOf("bar"),
          ),
        ),
      )

      val timeline = aggregate.getTimeline()

      assertThat(timeline).contains(
        TimelineItem(
          timestamp = LocalDateTime.parse("2025-01-01T12:00:00"),
          details = "2 answers updated and 0 removed",
        ),
        TimelineItem(
          timestamp = LocalDateTime.parse("2025-01-01T12:30:00"),
          details = "2 answers updated and 1 removed",
        ),
      )
      assertThat(aggregate.numberOfEventsApplied).isEqualTo(2)
    }

    @Test
    fun `it handles the AnswersRolledBack event`() {
      val aggregate = AssessmentTimelineAggregate()

      aggregate.apply(
        EventEntity(
          createdAt = LocalDateTime.parse("2025-01-01T13:00:00"),
          assessment = assessment,
          user = user,
          data = AssessmentAnswersRolledBackEvent(
            rolledBackTo = LocalDateTime.parse("2025-01-01T12:05:00"),
            added = mapOf(
              "foo" to listOf("foo_value"),
              "bar" to listOf("bar_value"),
            ),
            removed = listOf("baz"),
          ),
        ),
      )

      val timeline = aggregate.getTimeline()

      assertThat(timeline).contains(
        TimelineItem(
          timestamp = LocalDateTime.parse("2025-01-01T13:00:00"),
          details = "Rolled back 3 answers",
        ),
      )
      assertThat(aggregate.numberOfEventsApplied).isEqualTo(1)
    }

    @Test
    fun `it handles the AssessmentStatusUpdated event`() {
      val aggregate = AssessmentTimelineAggregate()

      aggregate.apply(
        EventEntity(
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          assessment = assessment,
          user = user,
          data = AssessmentStatusUpdatedEvent("INCOMPLETE"),
        ),
      )

      aggregate.apply(
        EventEntity(
          createdAt = LocalDateTime.parse("2025-01-01T13:00:00"),
          assessment = assessment,
          user = user,
          data = AssessmentStatusUpdatedEvent("COMPLETE"),
        ),
      )

      val timeline = aggregate.getTimeline()

      assertThat(timeline).contains(
        TimelineItem(
          timestamp = LocalDateTime.parse("2025-01-01T12:00:00"),
          details = "Assessment status changed to \"INCOMPLETE\"",
        ),
        TimelineItem(
          timestamp = LocalDateTime.parse("2025-01-01T13:00:00"),
          details = "Assessment status changed from \"INCOMPLETE\" to \"COMPLETE\"",
        ),
      )
      assertThat(aggregate.numberOfEventsApplied).isEqualTo(2)
    }
  }

  @Nested
  inner class ShouldCreate {
    @Test
    fun `it returns true when it updates on an event`() {
      val aggregate = AssessmentTimelineAggregate()

      assertThat(aggregate.shouldCreate(AssessmentCreatedEvent::class)).isEqualTo(true)
    }

    @Test
    fun `it returns false when it does update on an event`() {
      val aggregate = AssessmentTimelineAggregate()

      listOf(
        AssessmentAnswersUpdatedEvent::class,
        AssessmentAnswersRolledBackEvent::class,
        FormVersionUpdatedEvent::class,
        AssessmentStatusUpdatedEvent::class,
      ).forEach { event -> assertThat(aggregate.shouldCreate(event)).isEqualTo(false) }
    }

    @Test
    fun `returns true when it reaches a threshold`() {
      val aggregate = AssessmentTimelineAggregate()
      val threshold = 50

      (1..threshold).forEach {
        aggregate.apply(
          EventEntity(
            user = user,
            assessment = assessment,
            data = AssessmentAnswersUpdatedEvent(
              added = emptyMap(),
              removed = emptyList(),
            ),
          ),
        )

        assertThat(aggregate.shouldCreate(AssessmentAnswersUpdatedEvent::class))
          .withFailMessage("Failed on iteration $it, aggregate has ${aggregate.numberOfEventsApplied} applied")
          .isEqualTo(it == threshold)
      }
    }
  }

  @Nested
  inner class ShouldUpdate {
    @Test
    fun `it returns true when it updates on an event`() {
      listOf(
        AssessmentAnswersUpdatedEvent(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
        AssessmentAnswersRolledBackEvent(
          added = mapOf("foo" to listOf("previous_foo_value")),
          removed = emptyList(),
          rolledBackTo = LocalDateTime.now().minus(1, ChronoUnit.DAYS),
        ),
        AssessmentStatusUpdatedEvent("foo_event"),
      ).forEach { assertThat(AssessmentTimelineAggregate().shouldUpdate(it::class)).isEqualTo(true) }
    }

    @Test
    fun `it returns false when it does update on an event`() {
      listOf(
        AssessmentCreatedEvent(),
        FormVersionUpdatedEvent("updated_form_version"),
      ).forEach { assertThat(AssessmentTimelineAggregate().shouldUpdate(it::class)).isEqualTo(false) }
    }
  }

  @Nested
  inner class Clone {
    @Test
    fun `it clones the aggregate`() {
      val originalAggregate = AssessmentTimelineAggregate()

      originalAggregate.apply(
        EventEntity(
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          assessment = assessment,
          user = user,
          data = AssessmentAnswersUpdatedEvent(
            added = mapOf(
              "foo" to listOf("foo_value"),
              "bar" to listOf("bar_value"),
            ),
            removed = emptyList(),
          ),
        ),
      )

      val clonedAggregate = originalAggregate.clone()

      assertThat(clonedAggregate.getTimeline()).isEqualTo(originalAggregate.getTimeline())
    }
  }
}
