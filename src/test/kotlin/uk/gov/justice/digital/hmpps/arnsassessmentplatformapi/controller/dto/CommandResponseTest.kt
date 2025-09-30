package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.EventEntity
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AnswersUpdated
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.service.CommandExecutorResult
import java.util.UUID

class CommandResponseTest {
  @Test
  fun `it creates from a command execution result`() {
    val result = CommandExecutorResult(
      events = listOf(
        EventEntity(
          data = AnswersUpdated(added = mapOf("foo" to listOf("foo_value")), emptyList()),
          user = User("FOO_USER", "Foo User"),
          assessment = AssessmentEntity(),
        ),
      ),
      assessmentUuid = UUID.randomUUID(),
    )

    val response = CommandResponse.from(result)

    assertThat(response.assessmentUuid).isEqualTo(result.getAssessmentUuid())
  }
}
