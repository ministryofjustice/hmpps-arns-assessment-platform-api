package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AssessmentTimelineResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentTimelineAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import java.time.LocalDateTime
import kotlin.test.assertIs

class AssessmentTimelineMapperTest {
  @Test
  fun `it has an aggregate type`() {
    assertThat(AssessmentTimelineMapper().aggregateType).isEqualTo(AssessmentTimelineAggregate.aggregateType)
  }

  @Test
  fun `it maps an AssessmentTimelineAggregate in to a response`() {
    val assessment = AssessmentEntity()
    val user = User("FOO_USER", "Foo User")
    val aggregate = AssessmentTimelineAggregate().apply {
      apply(
        EventEntity(
          user = user,
          assessment = assessment,
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          data = AnswersUpdated(
            added = mapOf(
              "foo" to listOf("foo_value"),
              "bar" to listOf("bar_value"),
            ),
            removed = listOf(),
          ),
        ),
      )

      apply(
        EventEntity(
          user = user,
          assessment = assessment,
          createdAt = LocalDateTime.parse("2025-01-01T12:00:00"),
          data = AnswersUpdated(
            added = mapOf(
              "foo" to listOf("updated_foo_value"),
              "baz" to listOf("baz_value"),
            ),
            removed = listOf("bar"),
          ),
        ),
      )
    }

    val response = assertIs<AssessmentTimelineResponse>(AssessmentTimelineMapper().intoResponse(aggregate))
    assertThat(response.timeline.size).isEqualTo(2)
    assertThat(response.timeline).contains(
      TimelineItem(details = "2 answers updated and 0 removed", timestamp = LocalDateTime.parse("2025-01-01T12:00:00")),
      TimelineItem(details = "2 answers updated and 1 removed", timestamp = LocalDateTime.parse("2025-01-01T12:00:00")),
    )
  }

  @Test
  fun `it handles an empty timeline`() {
    val aggregate = AssessmentTimelineAggregate()

    val response = assertIs<AssessmentTimelineResponse>(AssessmentTimelineMapper().intoResponse(aggregate))

    assertThat(response.timeline.size).isEqualTo(0)
  }
}
