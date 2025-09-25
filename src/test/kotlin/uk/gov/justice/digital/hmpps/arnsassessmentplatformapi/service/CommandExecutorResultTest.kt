package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.exception.CommandExecutorResultException

class CommandExecutorResultTest {
  @Test
  fun `it holds an event stream and an assessment UUID`() {
    val assessment = AssessmentEntity()
    val events = listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        data = AssessmentCreated(),
      ),
    )

    val result = CommandExecutorResult(events, assessment.uuid)

    assertThat(result.getAssessmentUuid()).isEqualTo(assessment.uuid)
    assertThat(result.events).isEqualTo(events)
  }

  @Test
  fun `it throws when there are events but no assessment UUID`() {
    val assessment = AssessmentEntity()
    val events = listOf(
      EventEntity(
        user = User("FOO_USER", "Foo User"),
        assessment = assessment,
        data = AssessmentCreated(),
      ),
    )

    assertThrows<CommandExecutorResultException> { CommandExecutorResult(events) }
  }

  @Nested
  inner class IsOk

  @Nested
  inner class GetAssessmentUuid

  @Nested
  inner class MergeWith

  @Nested
  inner class GetEvents
}
