package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertIs

class AggregateEntityTest {
  @Test
  fun `test it works`() {
    val assessment = AssessmentEntity(createdAt = LocalDateTime.parse("2025-08-08T12:00:00"))
    val data = AssessmentVersionAggregate()

    val firstEvent = EventEntity(
      assessment = assessment,
      createdAt = LocalDateTime.parse("2025-08-08T12:00:00"),
      user = User(
        id = "foo-user",
        name = "Foo User",
      ),
      data = AnswersUpdated(
        added = mapOf("foo" to listOf("Original answer for foo")),
        removed = emptyList(),
      ),
    )

    val secondEvent = EventEntity(
      assessment = assessment,
      createdAt = LocalDateTime.parse("2025-08-08T13:30:00"),
      user = User(
        id = "foo-user",
        name = "Foo User",
      ),
      data = AnswersUpdated(
        added = mapOf("foo" to listOf("Updated value for foo")),
        removed = emptyList(),
      ),
    )

    val aggregate = AggregateEntity.init(
      assessment,
      data,
    )

    aggregate.apply(firstEvent)

    val originalData = assertIs<AssessmentVersionAggregate>(aggregate.data)
    assertThat(originalData.getAnswers()["foo"]).isEqualTo(listOf("Original answer for foo"))
    assertThat(aggregate.eventsFrom).isEqualTo(assessment.createdAt)
    assertThat(aggregate.eventsTo).isEqualTo(firstEvent.createdAt)

    aggregate.apply(secondEvent)

    val firstUpdate = assertIs<AssessmentVersionAggregate>(aggregate.data)
    assertThat(firstUpdate.getAnswers()["foo"]).isEqualTo(listOf("Updated value for foo"))
    assertThat(aggregate.eventsFrom).isEqualTo(assessment.createdAt)
    assertThat(aggregate.eventsTo).isEqualTo(secondEvent.createdAt)
  }
}
