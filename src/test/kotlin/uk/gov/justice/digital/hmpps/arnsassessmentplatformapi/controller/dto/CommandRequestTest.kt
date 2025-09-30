package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.User
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.CreateAssessment
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands.UpdateAnswers
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException
import java.util.UUID

class CommandRequestTest {
  @Test
  fun `it constructs when passed valid commands`() {
    CommandRequest(
      user = User("FOO_USER", "Foo User"),
      commands = listOf(UpdateAnswers(added = mapOf("foo" to listOf("foo_value")), removed = emptyList())),
      assessmentUuid = UUID.randomUUID(),
    )
  }

  @Test
  fun `it throws when initialised with no commands`() {
    assertThrows<InvalidCommandException> {
      CommandRequest(
        user = User("FOO_USER", "Foo User"),
        commands = emptyList(),
        assessmentUuid = UUID.randomUUID(),
      )
    }
  }

  @Test
  fun `it throws when initialised with an unsupported command`() {
    assertThrows<InvalidCommandException> {
      CommandRequest(
        user = User("FOO_USER", "Foo User"),
        commands = listOf(CreateAssessment()),
        assessmentUuid = UUID.randomUUID(),
      )
    }
  }
}
