package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.aggregates.AssessmentVersionResponse
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.aggregate.AssessmentVersionAggregate
import kotlin.test.assertIs

class AssessmentVersionMapperTest {
  @Test
  fun `it has an aggregate type`() {
    assertThat(AssessmentVersionMapper().aggregateType).isEqualTo(AssessmentVersionAggregate::class)
  }

  @Test
  fun `it maps an AssessmentVersionAggregate in to a response`() {
    val user = User("FOO_USER", "Foo User")

    val aggregate = AssessmentVersionAggregate(
      answers = mutableMapOf(
        "foo" to listOf("foo_value"),
        "baz" to listOf("baz_value"),
      ),
      deletedAnswers = mutableMapOf("bar" to listOf("bar_value")),
      collaborators = mutableSetOf(user),
      formVersion = "1",
    )

    val response = assertIs<AssessmentVersionResponse>(AssessmentVersionMapper().createResponseFrom(aggregate))

    assertThat(response.answers).isEqualTo(
      mapOf(
        "foo" to listOf("foo_value"),
        "baz" to listOf("baz_value"),
      ),
    )
    assertThat(response.collaborators).isEqualTo(
      setOf(
        user,
      ),
    )
    assertThat(response.formVersion).isEqualTo("1")
  }

  @Test
  fun `it handles an empty aggregate`() {
    val aggregate = AssessmentVersionAggregate(
      answers = mutableMapOf(),
      deletedAnswers = mutableMapOf(),
      collaborators = mutableSetOf(),
      formVersion = "1",
    )

    val response = assertIs<AssessmentVersionResponse>(AssessmentVersionMapper().createResponseFrom(aggregate))

    assertThat(response.answers.isEmpty()).isTrue
    assertThat(response.collaborators.isEmpty()).isTrue
    assertThat(response.formVersion).isEqualTo("1")
  }
}
