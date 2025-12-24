package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import java.time.LocalDateTime
import java.util.UUID

class AssessmentStateTest {
  val assessment = AssessmentEntity(type = "TEST")

  @Test
  fun `constructs taking an aggregate`() {
    val initialAggregate = AggregateEntity(
      eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
      numberOfEventsApplied = 1,
      assessment = assessment,
      data = AssessmentAggregate(),
    )

    val state = AssessmentState(initialAggregate)
    assertThat(state.aggregates).containsOnly(initialAggregate)
  }

  @Nested
  inner class Get {
    @Test
    fun `gets the latest aggregate`() {
      val latestAggregateUuid = UUID.randomUUID()
      val state = AssessmentState(
        aggregates = mutableListOf(
          AggregateEntity(
            eventsTo = LocalDateTime.parse("2025-01-01T12:10:00"),
            numberOfEventsApplied = 2,
            assessment = assessment,
            data = AssessmentAggregate(),
          ),
          AggregateEntity(
            eventsTo = LocalDateTime.parse("2025-01-01T12:15:00"),
            numberOfEventsApplied = 50,
            assessment = assessment,
            data = AssessmentAggregate(),
          ),
          AggregateEntity(
            eventsTo = LocalDateTime.parse("2025-01-01T12:15:00"),
            uuid = latestAggregateUuid,
            numberOfEventsApplied = 0,
            assessment = assessment,
            data = AssessmentAggregate(),
          ),
          AggregateEntity(
            eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
            numberOfEventsApplied = 1,
            assessment = assessment,
            data = AssessmentAggregate(),
          ),
        ),
      )

      val latest = state.getForRead()
      assertThat(latest.uuid).isEqualTo(latestAggregateUuid)
    }

    @Test
    fun `clones the latest aggregate if the number of events applied reaches the threshold`() {
      val latestAggregate = AggregateEntity(
        eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
        numberOfEventsApplied = 50,
        assessment = assessment,
        data = AssessmentAggregate().apply {
          formVersion = "1"
          answers.put("foo", SingleValue(UUID.randomUUID().toString()))
        },
      )
      val state = AssessmentState(
        aggregates = mutableListOf(latestAggregate),
      )

      val latest = state.getForWrite()
      assertThat(latest.uuid).isNotEqualTo(latestAggregate.uuid)
      assertThat(latest.data).usingRecursiveComparison().isEqualTo(latestAggregate.data)
      assertThat(state.aggregates.size).isEqualTo(2)
    }

    @Test
    fun `does not clone the latest aggregate if the number of events applied has reached the threshold but the aggregate is not fetched for update`() {
      val latestAggregate = AggregateEntity(
        eventsTo = LocalDateTime.parse("2025-01-01T12:00:00"),
        numberOfEventsApplied = 50,
        assessment = assessment,
        data = AssessmentAggregate().apply {
          formVersion = "1"
          answers.put("foo", SingleValue(UUID.randomUUID().toString()))
        },
      )
      val state = AssessmentState(
        aggregates = mutableListOf(latestAggregate),
      )

      val latest = state.getForRead()
      assertThat(latest.uuid).isEqualTo(latestAggregate.uuid)
      assertThat(latest.data).usingRecursiveComparison().isEqualTo(latestAggregate.data)
      assertThat(state.aggregates.size).isEqualTo(1)
    }
  }
}
