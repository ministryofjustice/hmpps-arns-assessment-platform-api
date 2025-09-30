package uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.controller.dto.commands

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnsassessmentplatformapi.persistence.entity.event.AssessmentCreated
import kotlin.test.assertIs

class CreateAssessmentTest {
  @Test
  fun `it can be transformed to an event`() {
    val event = CreateAssessment().toEvent()
    assertIs<AssessmentCreated>(event)
  }
}
