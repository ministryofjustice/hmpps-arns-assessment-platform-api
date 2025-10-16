package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.aggregate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersRolledBack
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.AssessmentStatusUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.event.FormVersionUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AssessmentVersionAggregateTest {
  @Nested
  inner class GetFormVersion {
    @Test
    fun `it returns the form version`() {
      val aggregate = AssessmentVersionAggregate(formVersion = "form_version")

      assertThat(aggregate.getFormVersion()).isEqualTo("form_version")
    }
  }

  @Nested
  inner class GetAnswers {
    @Test
    fun `it returns the answers`() {
      val answers = mutableMapOf(
        "foo" to listOf("foo_value"),
      )

      val aggregate = AssessmentVersionAggregate(answers = answers)

      assertThat(aggregate.getAnswers()).isEqualTo(answers)
    }
  }

  @Nested
  inner class Apply {
    @Test
    fun `it handles an AnswersUpdated event`() {
      val answers = mutableMapOf(
        "foo" to listOf("foo_value"),
        "bar" to listOf("bar_value"),
        "baz" to listOf("baz_value"),
      )

      val aggregate = AssessmentVersionAggregate(answers = answers)

      aggregate.apply(
        EventEntity(
          user = User("FOO_USER", "Foo User"),
          assessment = AssessmentEntity(),
          data = AnswersUpdated(
            added = mapOf("foo" to listOf("updated_foo_value")),
            removed = listOf("baz"),
          ),
        ),
      )

      val updatedAnswers = aggregate.getAnswers()

      assertThat(updatedAnswers["foo"]).isEqualTo(listOf("updated_foo_value"))
      assertThat(updatedAnswers["bar"]).isEqualTo(listOf("bar_value"))
      assertThat(updatedAnswers["baz"]).isNull()
    }

    @Test
    fun `it handles an AnswersRolledBack event`() {
      val answers = mutableMapOf(
        "foo" to listOf("foo_value"),
        "baz" to listOf("baz_value"),
      )

      val aggregate = AssessmentVersionAggregate(answers = answers)

      val now = LocalDateTime.now()

      aggregate.apply(
        EventEntity(
          user = User("FOO_USER", "Foo User"),
          assessment = AssessmentEntity(),
          data = AnswersRolledBack(
            added = mapOf("foo" to listOf("previous_foo_value"), "bar" to listOf("bar_value")),
            removed = listOf("baz"),
            rolledBackTo = now,
          ),
        ),
      )

      val updatedAnswers = aggregate.getAnswers()

      assertThat(updatedAnswers["foo"]).isEqualTo(listOf("previous_foo_value"))
      assertThat(updatedAnswers["bar"]).isEqualTo(listOf("bar_value"))
      assertThat(updatedAnswers["baz"]).isNull()
    }

    @Test
    fun `it handles an FormVersionUpdated event`() {
      val aggregate = AssessmentVersionAggregate(formVersion = "form_version")

      aggregate.apply(
        EventEntity(
          user = User("FOO_USER", "Foo User"),
          assessment = AssessmentEntity(),
          data = FormVersionUpdated("updated_form_version"),
        ),
      )

      val formVersion = aggregate.getFormVersion()

      assertThat(formVersion).isEqualTo("updated_form_version")
    }

    @Test
    fun `it does not apply other events`() {
      val aggregate = AssessmentVersionAggregate(
        formVersion = "original_form_version",
        answers = mutableMapOf("foo" to listOf("foo_value")),
      )

      val originalAggregate = aggregate.clone()

      originalAggregate.apply(
        EventEntity(
          user = User("FOO_USER", "Foo User"),
          assessment = AssessmentEntity(),
          data = AssessmentCreated(),
        ),
      )
    }
  }

  @Nested
  inner class ShouldUpdate {
    @Test
    fun `it returns true when it updates on an event`() {
      listOf(
        AnswersUpdated(
          added = mapOf("foo" to listOf("foo_value")),
          removed = emptyList(),
        ),
        AnswersRolledBack(
          added = mapOf("foo" to listOf("previous_foo_value")),
          removed = emptyList(),
          rolledBackTo = LocalDateTime.now().minus(1, ChronoUnit.DAYS),
        ),
        FormVersionUpdated("updated_form_version"),
      ).forEach { assertThat(AssessmentVersionAggregate().shouldUpdate(it::class)).isEqualTo(true) }
    }

    @Test
    fun `it returns false when it does update on an event`() {
      listOf(
        AssessmentCreated(),
        AssessmentStatusUpdated("foo_event"),
      ).forEach { assertThat(AssessmentVersionAggregate().shouldUpdate(it::class)).isEqualTo(false) }
    }
  }

  @Nested
  inner class Clone {
    @Test
    fun `it clones the aggregate`() {
      val originalAggregate = AssessmentVersionAggregate(
        formVersion = "original_form_version",
        answers = mutableMapOf("foo" to listOf("foo_value")),
      )

      val clonedAggregate = originalAggregate.clone()

      assertThat(clonedAggregate.getAnswers()).isEqualTo(originalAggregate.getAnswers())
      assertThat(clonedAggregate.getFormVersion()).isEqualTo(originalAggregate.getFormVersion())
    }
  }
}
