package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException
import java.util.UUID
import kotlin.test.Test

class CommandRequestTest {
  @Test
  fun `it creates`() {
    val updateAnswers = UpdateAnswers(
      user = User("FOO_USER", "Foo User"),
      assessmentUuid = UUID.randomUUID(),
      added = mapOf("foo" to listOf("foo_value")),
      removed = emptyList(),
    )

    val request = CommandsRequest(
      commands = listOf(updateAnswers),
    )

    Assertions.assertThat(request.commands).contains(updateAnswers)
  }

  @Test
  fun `it throws when passed no commands`() {
    assertThrows<InvalidCommandException> {
      CommandsRequest(
        commands = emptyList(),
      )
    }
  }
}
