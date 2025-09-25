package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.FormVersionUpdated
import kotlin.test.assertIs

class UpdateFormVersionTest {
  @Test
  fun `it can be transformed to an event`() {
    val event = UpdateFormVersion("foo_version").toEvent()

    assertIs<FormVersionUpdated>(event)
  }
}
