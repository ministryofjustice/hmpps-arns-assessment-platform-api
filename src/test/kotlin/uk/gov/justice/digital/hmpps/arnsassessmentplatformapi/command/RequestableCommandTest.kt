package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.UserDetails
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.common.toReference
import java.util.UUID

class RequestableCommandTest {
  private val user = UserDetails("USER", "Test User")
  private val assessmentUuid = UUID.randomUUID().toReference()

  @Test
  fun `non-whitelisted commands use the interface autosaved default`() {
    val command = UpdateFormVersionCommand(user, assessmentUuid, version = "1")

    assertThat(command.autosaved).isFalse()
  }

  @Test
  fun `whitelisted commands default autosaved to false and accept true`() {
    assertThat(UpdateFlagsCommand(user, assessmentUuid, emptyList()).autosaved).isFalse()
    assertThat(UpdateFlagsCommand(user, assessmentUuid, emptyList(), autosaved = true).autosaved).isTrue()
    assertThat(TestableRequestableCommand(user, assessmentUuid, autosaved = true).autosaved).isTrue()
  }
}
