package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.handler

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.AssessmentAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate.assessment.model.TimelineItem
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AggregateEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.AssessmentTimelineQuery
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.query.result.AssessmentTimelineQueryResult
import java.time.LocalDateTime

class AssessmentTimelineQueryHandlerTest : AbstractQueryHandlerTest() {
  override val handler = AssessmentTimelineQueryHandler::class

  val now: LocalDateTime = LocalDateTime.now()

  val allTimelineItems = listOf(
    TimelineItem(
      type = "FOO",
      createdAt = now,
      data = mapOf("foo" to "bar"),
    ),
    TimelineItem(
      type = "BAR",
      createdAt = now,
      data = mapOf("bar" to "foo"),
    ),
    TimelineItem(
      type = "FOO",
      createdAt = now,
      data = mapOf("foo" to "baz"),
    ),
    TimelineItem(
      type = "BAZ",
      createdAt = now,
      data = mapOf("baz" to "foo"),
    ),
  )

  val aggregate = AggregateEntity(
    assessment = assessment,
    data = AssessmentAggregate().apply {
      formVersion = "1"
      timeline.addAll(allTimelineItems)
    },
  )

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns unfiltered timeline data for a point in time`(timestamp: LocalDateTime?) {
    val query = AssessmentTimelineQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      timestamp = timestamp,
    )

    val expectedResult = AssessmentTimelineQueryResult(
      timeline = allTimelineItems.toMutableList(),
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns filtered timeline data for a single timeline type and a point in time`(timestamp: LocalDateTime?) {
    val query = AssessmentTimelineQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      timestamp = timestamp,
      timelineTypes = listOf("FOO"),
    )

    val expectedResult = AssessmentTimelineQueryResult(
      timeline = mutableListOf(
        TimelineItem(
          type = "FOO",
          createdAt = now,
          data = mapOf("foo" to "bar"),
        ),
        TimelineItem(
          type = "FOO",
          createdAt = now,
          data = mapOf("foo" to "baz"),
        ),
      ),
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns filtered timeline data for multiple timeline types and a point in time`(timestamp: LocalDateTime?) {
    val query = AssessmentTimelineQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      timestamp = timestamp,
      timelineTypes = listOf("BAR", "BAZ"),
    )

    val expectedResult = AssessmentTimelineQueryResult(
      timeline = mutableListOf(
        TimelineItem(
          type = "BAR",
          createdAt = now,
          data = mapOf("bar" to "foo"),
        ),
        TimelineItem(
          type = "BAZ",
          createdAt = now,
          data = mapOf("baz" to "foo"),
        ),
      ),
    )

    test(query, aggregate, expectedResult)
  }

  @ParameterizedTest
  @MethodSource("timestampProvider")
  fun `returns empty timeline data for a non-existent timeline type and a point in time`(timestamp: LocalDateTime?) {
    val query = AssessmentTimelineQuery(
      user = user,
      assessmentUuid = assessment.uuid,
      timestamp = timestamp,
      timelineTypes = listOf("TEST"),
    )

    val expectedResult = AssessmentTimelineQueryResult(
      timeline = mutableListOf(),
    )

    test(query, aggregate, expectedResult)
  }
}
