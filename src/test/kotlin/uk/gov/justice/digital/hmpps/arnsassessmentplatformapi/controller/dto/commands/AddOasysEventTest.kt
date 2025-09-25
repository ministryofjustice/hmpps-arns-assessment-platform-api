package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.OasysEventAdded
import kotlin.test.assertIs

class AddOasysEventTest {
  @Test
  fun `it can be transformed to an event`() {
    val event = AddOasysEvent("foo_oasys_event").toEvent()
    assertIs<OasysEventAdded>(event)
  }
}
