package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.request

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.exception.InvalidCommandException
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.model.SingleValue
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandsRequestTest {
  @Test
  fun `it creates`() {
    val updateAnswersCommand = UpdateAssessmentAnswersCommand(
      user = UserDetails("test-user", "Test User"),
      assessmentUuid = UUID.randomUUID(),
      added = mapOf("foo" to SingleValue("foo_value")),
      removed = emptyList(),
    )

    val request = CommandsRequest(
      commands = listOf(updateAnswersCommand),
    )

    Assertions.assertThat(request.commands).contains(updateAnswersCommand)
  }

  @Test
  fun `it throws when passed no commands`() {
    val exception = assertThrows<InvalidCommandException> {
      CommandsRequest(
        commands = emptyList(),
      )
    }
    assertEquals("No commands received", exception.developerMessage)
    assertEquals("Unable to process commands", exception.message)
  }
}
